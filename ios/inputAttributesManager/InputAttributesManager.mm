#import "InputAttributesManager.h"
#import "AttributeEntry.h"
#import "EnrichedTextInputView.h"
#import "LineHeightUtils.h"
#import "ParagraphAttributesUtils.h"
#import "RangeUtils.h"
#import "StyleHeaders.h"
#import "TextListsUtils.h"
#import "ZeroWidthSpaceUtils.h"

static NSString *const EnrichedAlignmentMarkerPrefix = @"EnrichedAlignment";

@implementation InputAttributesManager {
  NSMutableArray<NSValue *> *_dirtyRanges;
  NSSet *_customAttributesKeys;
  NSMutableSet *_removedTypingAttributes;
}

- (instancetype)initWithInput:(EnrichedTextInputView *)input {
  self = [super init];
  _input = input;
  _dirtyRanges = [[NSMutableArray alloc] init];
  _removedTypingAttributes = [[NSMutableSet alloc] init];

  // setup customAttributes
  NSMutableSet *_customAttrsSet = [[NSMutableSet alloc] init];
  for (StyleBase *style in _input->stylesDict.allValues) {
    [_customAttrsSet addObject:[style getKey]];
  }
  _customAttributesKeys = _customAttrsSet;

  return self;
}
- (NSSet<NSString *> *)customAttributesKeys {
  return _customAttributesKeys;
}

- (void)addDirtyRange:(NSRange)range {
  [_dirtyRanges addObject:[NSValue valueWithRange:range]];
  _dirtyRanges = [[RangeUtils connectAndDedupeRanges:_dirtyRanges] mutableCopy];
}

- (NSArray *)getDirtyRanges {
  return _dirtyRanges;
}

- (void)shiftDirtyRangesWithEditedRange:(NSRange)editedRange
                         changeInLength:(NSInteger)delta {
  if (delta == 0) {
    return;
  }
  NSArray *shiftedRanges = [RangeUtils shiftRanges:_dirtyRanges
                                   withEditedRange:editedRange
                                    changeInLength:delta];
  _dirtyRanges =
      [[RangeUtils connectAndDedupeRanges:shiftedRanges] mutableCopy];
}

- (void)didRemoveTypingAttribute:(NSString *)key {
  [_removedTypingAttributes addObject:key];
}

- (void)clearRemovedTypingAttributes {
  [_removedTypingAttributes removeAllObjects];
}

- (void)handleDirtyRangesStyling {
  // Filter out 0 length ranges for styling.
  NSPredicate *predicate = [NSPredicate
      predicateWithBlock:^BOOL(NSValue *evaluatedObject, NSDictionary *_) {
        return [evaluatedObject rangeValue].length > 0;
      }];
  [_dirtyRanges filterUsingPredicate:predicate];

  for (NSValue *rangeObj in _dirtyRanges) {
    NSRange dirtyRange = [rangeObj rangeValue];

    // dirty range can sometimes be wrong because of apple doing some changes
    // behind the scenes
    if (dirtyRange.location + dirtyRange.length >
        _input->textView.textStorage.string.length)
      continue;

    // firstly, get all styles' occurences in that dirty range
    NSMutableDictionary *presentStyles = [[NSMutableDictionary alloc] init];
    for (StyleBase *style in _input->stylesDict.allValues) {
      // the dict has keys of StyleType NSNumber and values of an array of all
      // occurences
      presentStyles[@([[style class] getType])] = [style all:dirtyRange];
    }

    // now reset the attributes to default ones
    [_input->textView.textStorage setAttributes:_input->defaultTypingAttributes
                                          range:dirtyRange];

    // Restore ZWS layout metadata that is stored in regular attributes and was
    // overwritten by the default-attributes reset above.
    [ZeroWidthSpaceUtils applyKernForZeroWidthSpacesInRange:dirtyRange
                                                       host:_input];

    // Sort style types so paragraph styles come first. Their broad visual
    // attributes (e.g. foreground color, font) are laid down before inline
    // styles override them on their specific sub-ranges. Styles of the same
    // kind apply in the StyleType order so the styling stays deterministic
    // (e.g. inline code's monospaced font wins over a custom font family).
    NSArray *sortedStyleTypes = [presentStyles.allKeys
        sortedArrayUsingComparator:^NSComparisonResult(NSNumber *a,
                                                       NSNumber *b) {
          BOOL aPara = [_input->stylesDict[a] isParagraph];
          BOOL bPara = [_input->stylesDict[b] isParagraph];
          if (aPara == bPara)
            return [a compare:b];
          return aPara ? NSOrderedAscending : NSOrderedDescending;
        }];

    // re-apply meta-attributes and apply visual styling following the saved
    // occurences.
    for (NSNumber *styleType in sortedStyleTypes) {
      StyleBase *style = _input->stylesDict[styleType];
      if (style == nullptr)
        continue;

      for (StylePair *stylePair in presentStyles[styleType]) {
        NSRange occurenceRange = [stylePair.rangeValue rangeValue];
        [style reapplyFromStylePair:stylePair];
        [style applyStyling:occurenceRange];
      }
    }
    [LineHeightUtils
        applyBaselineOffsetsInTextStorage:_input->textView.textStorage
                                    range:dirtyRange];
  }
  // do the typing attributes management, with no selection
  [self manageTypingAttributesWithOnlySelection:NO];

  [_dirtyRanges removeAllObjects];
}

- (void)manageTypingAttributesWithOnlySelection:(BOOL)onlySelectionChanged {
  EnrichedInputTextView *textView = _input->textView;
  NSRange selectedRange = textView.selectedRange;
  NSString *storageString = textView.textStorage.string;
  NSCharacterSet *newlines = [NSCharacterSet newlineCharacterSet];

  [self clearTrailingParagraphAttributesIfNeeded];

  // An empty paragraph types with its own styles. A moved caret always takes
  // them, other updates only when the paragraph's newline carries text styles,
  // so typing right after Enter still inherits the previous line.
  if (selectedRange.length == 0) {
    NSDictionary *paragraphAttributes =
        [self emptyParagraphAttributesAt:selectedRange.location
                       requireTextStyles:!onlySelectionChanged];
    if (paragraphAttributes != nullptr) {
      NSMutableDictionary *newAttrs =
          [[self typingAttributesFromParagraphAttributes:paragraphAttributes]
              mutableCopy];
      if (!onlySelectionChanged) {
        // Inline styles toggled at the caret (bold, italic...) stay active.
        NSSet<NSString *> *paragraphKeys = [self paragraphAttributeKeys];
        for (NSString *key in textView.typingAttributes) {
          if ([_customAttributesKeys containsObject:key] &&
              ![paragraphKeys containsObject:key]) {
            newAttrs[key] = textView.typingAttributes[key];
          }
        }
      }
      textView.typingAttributes = newAttrs;
      [self storeTrailingParagraphAttributesIfNeeded];
      return;
    }
  }

  // Typing attributes get reset (except alignment) when only selection changed
  // to an empty line (or empty line with newline) that has no own styles.
  if (onlySelectionChanged) {
    NSRange paragraphRange =
        [storageString paragraphRangeForRange:selectedRange];
    // User changed selection to an empty line (or empty line with a newline).
    if (paragraphRange.length == 0 ||
        (paragraphRange.length == 1 &&
         [newlines characterIsMember:[storageString
                                         characterAtIndex:paragraphRange
                                                              .location]])) {
      NSParagraphStyle *currentTypingStyle =
          textView.typingAttributes[NSParagraphStyleAttributeName];
      NSTextAlignment savedAlignment = currentTypingStyle
                                           ? currentTypingStyle.alignment
                                           : NSTextAlignmentNatural;

      [ParagraphAttributesUtils resetTypingAttributes:_input
                                  preservingAlignment:savedAlignment];
      [self storeTrailingParagraphAttributesIfNeeded];
      return;
    }
  }

  // General typing attributes management.

  // Firstly, we make sure only default + custom + paragraph typing attribtues
  // are left.
  NSMutableDictionary *newAttrs = [_input->defaultTypingAttributes mutableCopy];

  for (NSString *key in _input->textView.typingAttributes.allKeys) {
    if ([_customAttributesKeys containsObject:key]) {
      if ([key isEqualToString:NSParagraphStyleAttributeName]) {
        // NSParagraphStyle for paragraph styles -> keep list markers and
        // direct paragraph alignment.
        NSParagraphStyle *pStyle =
            (NSParagraphStyle *)_input->textView
                .typingAttributes[NSParagraphStyleAttributeName];
        if (pStyle != nullptr && (pStyle.textLists.count >= 1 ||
                                  pStyle.alignment != NSTextAlignmentNatural)) {
          NSMutableParagraphStyle *newPStyle =
              [[NSMutableParagraphStyle alloc] init];
          newPStyle.textLists = [TextListsUtils
              textListsByRemovingPrefix:EnrichedAlignmentMarkerPrefix
                              fromArray:pStyle.textLists];
          newPStyle.alignment = pStyle.alignment;
          newAttrs[NSParagraphStyleAttributeName] = newPStyle;
        }
      } else {
        // Inline styles -> keep the key/value as a whole
        newAttrs[key] = _input->textView.typingAttributes[key];
      }
    }
  }

  // Then, we add typingAttributes from present inline styles.
  // We check for the previous character to naturally extend typing attributes.
  // getEntryIfPresent properly returns nullptr for styles that we don't want to
  // extend this way. Attributes from _removedTypingAttributes aren't added
  // because they were just removed.
  BOOL startsNonEmptyParagraph =
      selectedRange.location < storageString.length &&
      ![newlines
          characterIsMember:[storageString
                                characterAtIndex:selectedRange.location]] &&
      (selectedRange.location == 0 ||
       [newlines
           characterIsMember:[storageString
                                 characterAtIndex:selectedRange.location - 1]]);
  BOOL hasSourceCharacter =
      startsNonEmptyParagraph || selectedRange.location > 0;
  NSUInteger sourceIndex = startsNonEmptyParagraph ? selectedRange.location
                                                   : selectedRange.location - 1;

  for (StyleBase *style in _input->stylesDict.allValues) {
    if ([style isParagraph])
      continue;
    if ([_removedTypingAttributes containsObject:[style getKey]])
      continue;

    AttributeEntry *entry = nullptr;

    if (hasSourceCharacter) {
      entry = [style getEntryIfPresent:NSMakeRange(sourceIndex, 1)];
    }

    if (entry == nullptr)
      continue;

    newAttrs[entry.key] = entry.value;
  }

  // Apply active styles to typing attributes only for styles that require it so
  // the cursor correctly reflects the current formatting state (e.g. heading
  // size).
  for (StyleBase *style in _input->stylesDict.allValues) {
    if ([style appliesStylingToTyping] && [style detect:selectedRange]) {
      [style applyStylingToTypingAttrs:newAttrs];
    }
  }

  textView.typingAttributes = newAttrs;

  [self storeTrailingParagraphAttributesIfNeeded];
}

// The inline text styles in the order their typing styling is applied in.
- (NSArray<TextStyleBase *> *)textStyles {
  NSMutableArray<TextStyleBase *> *styles = [[NSMutableArray alloc] init];
  for (NSNumber *type in [TextStyleBase textStyleTypes]) {
    TextStyleBase *style = _input->stylesDict[type];
    if (style != nullptr) {
      [styles addObject:style];
    }
  }
  return styles;
}

// Keys of the attributes an empty paragraph stores as its own styles.
- (NSSet<NSString *> *)paragraphAttributeKeys {
  NSMutableSet<NSString *> *keys = [[NSMutableSet alloc] init];
  [keys addObject:NSParagraphStyleAttributeName];
  for (TextStyleBase *style in [self textStyles]) {
    [keys addObject:[style getKey]];
  }
  StyleBase *marginStyle =
      _input->stylesDict[@([ParagraphMarginStyle getType])];
  if (marginStyle != nullptr) {
    [keys addObject:[marginStyle getKey]];
  }
  return keys;
}

- (NSDictionary *)textStyleValuesAt:(NSUInteger)location {
  NSMutableDictionary *values = [[NSMutableDictionary alloc] init];
  for (TextStyleBase *style in [self textStyles]) {
    NSString *value = [style getValueAt:location];
    if (value != nullptr) {
      values[[style getKey]] = value;
    }
  }
  return values;
}

// An empty paragraph's newline without text styles of its own takes the ones
// it was typing with (e.g. inherited after Enter) before one of them changes.
- (void)storeTypingTextStylesOnEmptyParagraphAt:(NSUInteger)location {
  if ([self textStyleValuesAt:location].count > 0) {
    return;
  }
  NSDictionary *typingAttributes = _input->textView.typingAttributes;
  for (TextStyleBase *style in [self textStyles]) {
    NSString *value = typingAttributes[[style getKey]];
    if (value != nullptr) {
      [style add:NSMakeRange(location, 1)
               withValue:value
              withTyping:NO
          withDirtyRange:YES];
    }
  }
}

- (BOOL)isInTrailingEmptyParagraph:(NSRange)range {
  NSString *string = _input->textView.textStorage.string;
  return range.length == 0 && range.location == string.length &&
         (string.length == 0 ||
          [[NSCharacterSet newlineCharacterSet]
              characterIsMember:[string characterAtIndex:string.length - 1]]);
}

// Returns the stored styles of the empty paragraph at the location or nullptr
// when there is no empty paragraph (or it has nothing usable stored).
- (NSDictionary *)emptyParagraphAttributesAt:(NSUInteger)location
                           requireTextStyles:(BOOL)requireTextStyles {
  NSString *string = _input->textView.textStorage.string;
  if ([self isInTrailingEmptyParagraph:NSMakeRange(location, 0)]) {
    return _trailingParagraphAttributes;
  }

  NSCharacterSet *newlines = [NSCharacterSet newlineCharacterSet];
  if (location >= string.length ||
      ![newlines characterIsMember:[string characterAtIndex:location]] ||
      (location > 0 &&
       ![newlines characterIsMember:[string characterAtIndex:location - 1]])) {
    return nullptr;
  }

  NSDictionary *attributes =
      [_input->textView.textStorage attributesAtIndex:location
                                       effectiveRange:nullptr];
  // Lists, quotes and headings keep their own typing attributes handling.
  NSParagraphStyle *pStyle = attributes[NSParagraphStyleAttributeName];
  if ([TextListsUtils textListsByRemovingPrefix:EnrichedAlignmentMarkerPrefix
                                      fromArray:pStyle.textLists]
          .count > 0) {
    return nullptr;
  }

  if (requireTextStyles && [self textStyleValuesAt:location].count == 0) {
    return nullptr;
  }
  return attributes;
}

- (NSDictionary *)typingAttributesFromParagraphAttributes:
    (NSDictionary *)paragraphAttributes {
  NSMutableDictionary *newAttrs = [_input->defaultTypingAttributes mutableCopy];

  NSMutableParagraphStyle *pStyle =
      [newAttrs[NSParagraphStyleAttributeName] mutableCopy]
          ?: [[NSMutableParagraphStyle alloc] init];
  pStyle.textLists =
      [TextListsUtils textListsByRemovingPrefix:EnrichedAlignmentMarkerPrefix
                                      fromArray:pStyle.textLists];
  NSParagraphStyle *paragraphStyle =
      paragraphAttributes[NSParagraphStyleAttributeName];
  pStyle.alignment =
      paragraphStyle ? paragraphStyle.alignment : NSTextAlignmentNatural;
  newAttrs[NSParagraphStyleAttributeName] = pStyle;

  NSArray<TextStyleBase *> *textStyles = [self textStyles];
  for (TextStyleBase *style in textStyles) {
    NSString *value = paragraphAttributes[[style getKey]];
    if (value != nullptr) {
      newAttrs[[style getKey]] = value;
    }
  }
  for (TextStyleBase *style in textStyles) {
    [style applyStylingToTypingAttrs:newAttrs];
  }

  ParagraphMarginStyle *marginStyle =
      _input->stylesDict[@([ParagraphMarginStyle getType])];
  NSString *margin = paragraphAttributes[[marginStyle getKey]];
  if (marginStyle != nullptr && margin.length > 0) {
    newAttrs[[marginStyle getKey]] = margin;
    [marginStyle applyStylingToTypingAttrs:newAttrs];
  }

  return newAttrs;
}

- (NSDictionary *)paragraphAttributesFromTypingAttributes:
    (NSDictionary *)typingAttributes {
  NSMutableDictionary *paragraphAttributes = [[NSMutableDictionary alloc] init];

  for (TextStyleBase *style in [self textStyles]) {
    NSString *value = typingAttributes[[style getKey]];
    if (value != nullptr) {
      paragraphAttributes[[style getKey]] = value;
    }
  }

  NSParagraphStyle *typingPStyle =
      typingAttributes[NSParagraphStyleAttributeName];
  NSMutableParagraphStyle *pStyle = [[NSMutableParagraphStyle alloc] init];
  pStyle.alignment =
      typingPStyle ? typingPStyle.alignment : NSTextAlignmentNatural;
  paragraphAttributes[NSParagraphStyleAttributeName] = pStyle;

  ParagraphMarginStyle *marginStyle =
      _input->stylesDict[@([ParagraphMarginStyle getType])];
  NSString *margin = typingAttributes[[marginStyle getKey]];
  if (marginStyle != nullptr && margin.length > 0) {
    paragraphAttributes[[marginStyle getKey]] = margin;
    [marginStyle applyStylingToTypingAttrs:paragraphAttributes];
  }

  return paragraphAttributes;
}

- (NSDictionary *)paragraphAttributesFromParsedParagraph:
    (NSDictionary *)parsedParagraph {
  NSMutableDictionary *paragraphAttributes = [[NSMutableDictionary alloc] init];

  NSDictionary<NSNumber *, NSString *> *textStyles =
      parsedParagraph[@"textStyles"];
  for (NSNumber *type in textStyles) {
    StyleBase *style = _input->stylesDict[type];
    if (style != nullptr) {
      paragraphAttributes[[style getKey]] = textStyles[type];
    }
  }

  NSMutableParagraphStyle *pStyle = [[NSMutableParagraphStyle alloc] init];
  NSNumber *alignment = parsedParagraph[@"alignment"];
  pStyle.alignment = alignment != nullptr
                         ? (NSTextAlignment)[alignment integerValue]
                         : NSTextAlignmentNatural;
  paragraphAttributes[NSParagraphStyleAttributeName] = pStyle;

  ParagraphMarginStyle *marginStyle =
      _input->stylesDict[@([ParagraphMarginStyle getType])];
  NSNumber *marginTop = parsedParagraph[@"marginTop"];
  NSNumber *marginBottom = parsedParagraph[@"marginBottom"];
  if (marginStyle != nullptr &&
      (marginTop != nullptr || marginBottom != nullptr)) {
    paragraphAttributes[[marginStyle getKey]] =
        [ParagraphMarginStyle valueWithMarginTop:marginTop
                                    marginBottom:marginBottom];
    [marginStyle applyStylingToTypingAttrs:paragraphAttributes];
  }

  return paragraphAttributes;
}

// While the caret is in the empty last paragraph, its stored styles follow the
// typing attributes.
- (void)storeTrailingParagraphAttributesIfNeeded {
  EnrichedInputTextView *textView = _input->textView;
  if (![self isInTrailingEmptyParagraph:textView.selectedRange]) {
    return;
  }
  self.trailingParagraphAttributes =
      [self paragraphAttributesFromTypingAttributes:textView.typingAttributes];
}

- (void)clearTrailingParagraphAttributesIfNeeded {
  NSString *string = _input->textView.textStorage.string;
  if (string.length > 0 &&
      ![[NSCharacterSet newlineCharacterSet]
          characterIsMember:[string characterAtIndex:string.length - 1]]) {
    self.trailingParagraphAttributes = nullptr;
  }
}

@end
