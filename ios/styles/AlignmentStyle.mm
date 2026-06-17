#import "AlignmentUtils.h"
#import "StyleHeaders.h"

@implementation AlignmentStyle

+ (StyleType)getType {
  return Alignment;
}

- (NSString *)getValue {
  return [AlignmentUtils alignmentToString:NSTextAlignmentNatural];
}

- (BOOL)isParagraph {
  return YES;
}

- (BOOL)appliesStylingToTyping {
  return YES;
}

- (void)toggle:(NSRange)range {
  // no-op for alignments
}

- (void)applyStyling:(NSRange)range {
  // Alignment is stored directly on NSParagraphStyle, so there is no separate
  // visual style to re-apply.
}

- (NSRange)actualUsedRange:(NSRange)range {
  NSRange paragraphRange =
      [self.host.textView.textStorage.string paragraphRangeForRange:range];
  return [self expandRangeToContiguousList:paragraphRange];
}

- (void)addAlignment:(NSTextAlignment)alignment
               range:(NSRange)range
          withTyping:(BOOL)withTyping
      withDirtyRange:(BOOL)withDirtyRange {
  NSRange actualRange = [self actualUsedRange:range];

  [self.host.textView.textStorage
      enumerateAttribute:NSParagraphStyleAttributeName
                 inRange:actualRange
                 options:0
              usingBlock:^(id _Nullable existingValue, NSRange subRange,
                           BOOL *_Nonnull stop) {
                NSMutableParagraphStyle *pStyle =
                    [(NSParagraphStyle *)existingValue mutableCopy];
                if (pStyle == nil) {
                  pStyle = [[NSMutableParagraphStyle alloc] init];
                }

                pStyle.alignment = alignment;
                [self.host.textView.textStorage
                    addAttribute:NSParagraphStyleAttributeName
                           value:pStyle
                           range:subRange];
              }];

  if (withTyping) {
    NSMutableDictionary *newTypingAttrs =
        [self.host.textView.typingAttributes mutableCopy];
    NSMutableParagraphStyle *pStyle =
        [newTypingAttrs[NSParagraphStyleAttributeName] mutableCopy];
    if (pStyle == nil) {
      pStyle = [[NSMutableParagraphStyle alloc] init];
    }
    pStyle.alignment = alignment;
    newTypingAttrs[NSParagraphStyleAttributeName] = pStyle;
    self.host.textView.typingAttributes = newTypingAttrs;
  }

  if (withDirtyRange) {
    [self.host.attributesManager addDirtyRange:actualRange];
  }
}

- (BOOL)styleCondition:(id)value range:(NSRange)range {
  NSParagraphStyle *pStyle = (NSParagraphStyle *)value;
  if (pStyle == nil)
    return NO;
  return pStyle.alignment != NSTextAlignmentNatural;
}

- (void)reapplyFromStylePair:(StylePair *)pair {
  NSRange range = [pair.rangeValue rangeValue];
  NSParagraphStyle *savedPStyle = pair.styleValue;
  if (savedPStyle == nil || savedPStyle.alignment == NSTextAlignmentNatural) {
    return;
  }

  [self addAlignment:savedPStyle.alignment
               range:range
          withTyping:NO
      withDirtyRange:NO];
}

- (NSString *)getStyleState {
  UITextView *textView = self.host.textView;
  NSParagraphStyle *paraStyle =
      textView.typingAttributes[NSParagraphStyleAttributeName];

  return [AlignmentUtils alignmentToString:paraStyle ? paraStyle.alignment
                                                     : NSTextAlignmentNatural];
}

- (void)applyStylingToTypingAttrs:(NSMutableDictionary *)attributes {
  NSMutableParagraphStyle *pStyle =
      [attributes[NSParagraphStyleAttributeName] mutableCopy];
  if (pStyle == nil) {
    pStyle = [[NSMutableParagraphStyle alloc] init];
  }

  NSRange selectedRange = self.host.textView.selectedRange;
  NSString *text = self.host.textView.textStorage.string;
  if (text.length == 0) {
    attributes[NSParagraphStyleAttributeName] = pStyle;
    return;
  }

  NSUInteger location = MIN(selectedRange.location, text.length - 1);
  NSParagraphStyle *selectedPStyle =
      [self.host.textView.textStorage attribute:NSParagraphStyleAttributeName
                                        atIndex:location
                                 effectiveRange:nil];
  pStyle.alignment =
      selectedPStyle ? selectedPStyle.alignment : NSTextAlignmentNatural;
  attributes[NSParagraphStyleAttributeName] = pStyle;
}

- (NSRange)expandRangeToContiguousList:(NSRange)range {
  NSString *text = self.host.textView.textStorage.string;
  if (text.length == 0)
    return range;

  NSArray<StyleBase *> *listStyles = @[
    self.host.stylesDict[@([UnorderedListStyle getType])],
    self.host.stylesDict[@([OrderedListStyle getType])],
    self.host.stylesDict[@([CheckboxListStyle getType])]
  ];

  NSRange expandedRange = range;

  // Expand Backward
  NSRange startParagraph =
      [text paragraphRangeForRange:NSMakeRange(range.location, 0)];

  // Find which list style is active at the start
  StyleBase *activeStartStyle = nil;
  for (StyleBase *style in listStyles) {
    if ([style detect:startParagraph]) {
      activeStartStyle = style;
      break;
    }
  }

  // If we found a list style, walk backwards until it stops
  if (activeStartStyle) {
    NSRange currentPara = startParagraph;
    while (currentPara.location > 0) {
      // Check the paragraph before the current one
      NSRange prevPara = [text
          paragraphRangeForRange:NSMakeRange(currentPara.location - 1, 0)];

      if ([activeStartStyle detect:prevPara]) {
        // It's still the same list -> Expand our range.
        expandedRange = NSUnionRange(expandedRange, prevPara);
        currentPara = prevPara;
      } else {
        // The list ended here.
        break;
      }
    }
  }

  // Expand forward, we check the paragraph at the end of the current selection
  NSUInteger endLoc =
      (range.length > 0) ? (NSMaxRange(range) - 1) : range.location;
  NSRange endParagraph = [text paragraphRangeForRange:NSMakeRange(endLoc, 0)];

  // Find which list style is active at the end
  StyleBase *activeEndStyle = nil;
  for (StyleBase *style in listStyles) {
    if ([style detect:endParagraph]) {
      activeEndStyle = style;
      break;
    }
  }

  // If we found a list style, walk forwards until it stops
  if (activeEndStyle) {
    NSRange currentPara = endParagraph;
    while (NSMaxRange(currentPara) < text.length) {
      // Check the paragraph after the current one
      NSRange nextPara =
          [text paragraphRangeForRange:NSMakeRange(NSMaxRange(currentPara), 0)];

      if ([activeEndStyle detect:nextPara]) {
        // It's still the same list -> expand our range.
        expandedRange = NSUnionRange(expandedRange, nextPara);
        currentPara = nextPara;
      } else {
        break;
      }
    }
  }

  return expandedRange;
}

@end
