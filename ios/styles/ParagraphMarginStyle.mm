#import "StyleHeaders.h"

static NSString *const ParagraphMarginTopKey = @"top";
static NSString *const ParagraphMarginBottomKey = @"bottom";

@interface ParagraphMarginStyle ()
+ (NSString *)valueWithMarginTop:(NSNumber *)marginTop
                    marginBottom:(NSNumber *)marginBottom;
+ (NSDictionary<NSString *, NSNumber *> *)marginsFromValue:(NSString *)value;
@end

@implementation ParagraphMarginStyle

+ (StyleType)getType {
  return ParagraphMargin;
}

- (NSString *)getKey {
  return @"EnrichedParagraphMargin";
}

- (BOOL)isParagraph {
  return YES;
}

- (NSRange)actualUsedRange:(NSRange)range {
  return [self.host.textView.textStorage.string paragraphRangeForRange:range];
}

- (BOOL)styleCondition:(id)value range:(NSRange)range {
  return
      [value isKindOfClass:[NSString class]] && [(NSString *)value length] > 0;
}

- (void)add:(NSRange)range
         withValue:(NSString *)value
        withTyping:(BOOL)withTyping
    withDirtyRange:(BOOL)withDirtyRange {
  NSRange actualRange = [self actualUsedRange:range];
  if (actualRange.length == 0 || value.length == 0) {
    return;
  }

  [self.host.textView.textStorage addAttribute:[self getKey]
                                         value:value
                                         range:actualRange];

  if (withTyping) {
    [self addTypingWithValue:value];
  }

  if (withDirtyRange) {
    [self.host.attributesManager addDirtyRange:actualRange];
  }
}

- (void)remove:(NSRange)range withDirtyRange:(BOOL)withDirtyRange {
  NSRange actualRange = [self actualUsedRange:range];
  if (actualRange.length == 0) {
    return;
  }

  [self.host.textView.textStorage removeAttribute:[self getKey]
                                            range:actualRange];
  [self removeTyping];

  if (withDirtyRange) {
    [self.host.attributesManager addDirtyRange:actualRange];
  }
}

- (void)addTypingWithValue:(NSString *)value {
  NSMutableDictionary *newTypingAttrs =
      [self.host.textView.typingAttributes mutableCopy];
  newTypingAttrs[[self getKey]] = value;
  self.host.textView.typingAttributes = newTypingAttrs;
}

- (void)removeTyping {
  NSMutableDictionary *newTypingAttrs =
      [self.host.textView.typingAttributes mutableCopy];
  [newTypingAttrs removeObjectForKey:[self getKey]];
  [self.host.attributesManager didRemoveTypingAttribute:[self getKey]];
  self.host.textView.typingAttributes = newTypingAttrs;
}

- (void)addMarginTop:(NSNumber *)marginTop
        marginBottom:(NSNumber *)marginBottom
               range:(NSRange)range
          withTyping:(BOOL)withTyping
      withDirtyRange:(BOOL)withDirtyRange {
  NSString *value = [ParagraphMarginStyle valueWithMarginTop:marginTop
                                                marginBottom:marginBottom];
  [self add:range
           withValue:value
          withTyping:withTyping
      withDirtyRange:withDirtyRange];
  [self applyStyling:[self actualUsedRange:range]];
}

- (void)applyStyling:(NSRange)range {
  [self.host.textView.textStorage
      enumerateAttribute:[self getKey]
                 inRange:range
                 options:0
              usingBlock:^(id _Nullable value, NSRange subRange,
                           BOOL *_Nonnull stop) {
                if (![self styleCondition:value range:subRange]) {
                  return;
                }

                NSDictionary<NSString *, NSNumber *> *margins =
                    [ParagraphMarginStyle marginsFromValue:(NSString *)value];

                [self.host.textView.textStorage
                    enumerateAttribute:NSParagraphStyleAttributeName
                               inRange:subRange
                               options:0
                            usingBlock:^(id _Nullable paragraphValue,
                                         NSRange paragraphSubRange,
                                         BOOL *_Nonnull paragraphStop) {
                              NSMutableParagraphStyle *pStyle =
                                  [(NSParagraphStyle *)
                                          paragraphValue mutableCopy];
                              if (pStyle == nullptr) {
                                pStyle = [[NSMutableParagraphStyle alloc] init];
                              }

                              NSNumber *marginTop =
                                  margins[ParagraphMarginTopKey];
                              NSNumber *marginBottom =
                                  margins[ParagraphMarginBottomKey];

                              if (marginTop != nil) {
                                pStyle.paragraphSpacingBefore =
                                    [marginTop floatValue];
                              }

                              if (marginBottom != nil) {
                                pStyle.paragraphSpacing =
                                    [marginBottom floatValue];
                              }

                              [self.host.textView.textStorage
                                  addAttribute:NSParagraphStyleAttributeName
                                         value:pStyle
                                         range:paragraphSubRange];
                            }];
              }];
}

- (void)applyStylingToTypingAttrs:(NSMutableDictionary *)attributes {
  NSString *value = attributes[[self getKey]];
  if (value.length == 0) {
    return;
  }

  NSDictionary<NSString *, NSNumber *> *margins =
      [ParagraphMarginStyle marginsFromValue:value];
  NSMutableParagraphStyle *pStyle =
      [attributes[NSParagraphStyleAttributeName] mutableCopy];
  if (pStyle == nullptr) {
    pStyle = [[NSMutableParagraphStyle alloc] init];
  }

  NSNumber *marginTop = margins[ParagraphMarginTopKey];
  NSNumber *marginBottom = margins[ParagraphMarginBottomKey];

  if (marginTop != nil) {
    pStyle.paragraphSpacingBefore = [marginTop floatValue];
  }

  if (marginBottom != nil) {
    pStyle.paragraphSpacing = [marginBottom floatValue];
  }

  attributes[NSParagraphStyleAttributeName] = pStyle;
}

- (void)reapplyFromStylePair:(StylePair *)pair {
  NSString *value = (NSString *)pair.styleValue;
  if (value.length == 0) {
    return;
  }
  [self add:[pair.rangeValue rangeValue]
           withValue:value
          withTyping:NO
      withDirtyRange:NO];
}

+ (NSString *)valueWithMarginTop:(NSNumber *)marginTop
                    marginBottom:(NSNumber *)marginBottom {
  NSMutableArray<NSString *> *parts = [[NSMutableArray alloc] init];

  if (marginTop != nil) {
    [parts addObject:[NSString stringWithFormat:@"%@=%g", ParagraphMarginTopKey,
                                                [marginTop floatValue]]];
  }

  if (marginBottom != nil) {
    [parts
        addObject:[NSString stringWithFormat:@"%@=%g", ParagraphMarginBottomKey,
                                             [marginBottom floatValue]]];
  }

  return [parts componentsJoinedByString:@";"];
}

+ (NSDictionary<NSString *, NSNumber *> *)marginsFromValue:(NSString *)value {
  NSMutableDictionary<NSString *, NSNumber *> *margins =
      [[NSMutableDictionary alloc] init];

  for (NSString *part in [value componentsSeparatedByString:@";"]) {
    NSArray<NSString *> *pair = [part componentsSeparatedByString:@"="];
    if (pair.count != 2) {
      continue;
    }

    NSString *key = pair[0];
    NSString *rawValue = pair[1];
    if (rawValue.length == 0) {
      continue;
    }

    if ([key isEqualToString:ParagraphMarginTopKey] ||
        [key isEqualToString:ParagraphMarginBottomKey]) {
      margins[key] = @([rawValue floatValue]);
    }
  }

  return margins;
}

@end
