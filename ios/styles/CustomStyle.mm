#import "EnrichedTextInputView.h"
#import "extensions/ColorExtension.h"
#import "extensions/FontExtension.h"
#import "interfaces/CustomStyleData.h"
#import "interfaces/StyleHeaders.h"
#import "utils/LineBoxLayoutDelegate.h"
#import "utils/RangeUtils.h"
#import "utils/ZeroWidthSpaceUtils.h"

static NSString *const CustomStyleAttributeName = @"EnrichedCustomStyle";

@implementation CustomStyle

+ (StyleType)getType {
  return Custom;
}

- (NSString *)getKey {
  return CustomStyleAttributeName;
}

- (BOOL)isParagraph {
  return NO;
}

- (NSInteger)stylePriority {
  return 1;
}

- (BOOL)isFontFamilyAvailable:(NSString *)family {
  return [UIFont fontNamesForFamilyName:family].count > 0 ||
         [UIFont fontWithName:family size:UIFont.systemFontSize] != nil;
}

- (UIFont *)fontForAvailableFamily:(NSString *)family size:(CGFloat)size {
  UIFont *namedFont = [UIFont fontWithName:family size:size];
  if (namedFont != nil) {
    return namedFont;
  }
  NSString *shortestFaceName = nil;
  for (NSString *faceName in [UIFont fontNamesForFamilyName:family]) {
    if (shortestFaceName == nil || faceName.length < shortestFaceName.length) {
      shortestFaceName = faceName;
    }
  }
  return [UIFont fontWithName:shortestFaceName size:size];
}

- (UIFont *)resolveFont:(UIFont *)font forData:(CustomStyleData *)data {
  UIFont *resolvedFont = font ?: [self.host.config primaryFont];
  UIFont *originalFont = resolvedFont;

  if (data.fontFamily.length > 0) {
    // if the font family isn't available, we stick to the current font
    if ([self isFontFamilyAvailable:data.fontFamily]) {
      resolvedFont = [self fontForAvailableFamily:data.fontFamily
                                             size:resolvedFont.pointSize];
      resolvedFont = [resolvedFont withFontTraits:originalFont];
    } else {
      NSLog(@"[EnrichedTextInput] Font family '%@' not found, keeping the "
            @"current font.",
            data.fontFamily);
    }
  }

  if (data.fontSize != nil) {
    CGFloat pointSize =
        [self.host.config scaledFontSizeForValue:[data.fontSize doubleValue]];
    resolvedFont = [resolvedFont setSize:pointSize];
  }

  return resolvedFont;
}

- (NSNumber *)resolveKernForData:(CustomStyleData *)data {
  return @([self.host.config
      scaledFontSizeForValue:[data.letterSpacing doubleValue]]);
}

- (NSNumber *)resolveLineBoxHeightForData:(CustomStyleData *)data {
  return @(
      [self.host.config scaledFontSizeForValue:[data.lineHeight doubleValue]]);
}

- (NSParagraphStyle *)resolveParagraphStyle:(NSParagraphStyle *)paragraphStyle
                                    forData:(CustomStyleData *)data {
  NSMutableParagraphStyle *resolvedStyle =
      [paragraphStyle mutableCopy] ?: [[NSMutableParagraphStyle alloc] init];
  CGFloat lineHeight = [[self resolveLineBoxHeightForData:data] doubleValue];
  resolvedStyle.minimumLineHeight = lineHeight;
  resolvedStyle.maximumLineHeight = lineHeight;
  return resolvedStyle;
}

- (BOOL)appliesStylingToTyping {
  CustomStyleData *data =
      [self getCustomStyleDataAt:self.host.textView.selectedRange.location];
  return data.fontSize != nil || data.fontFamily.length > 0 ||
         data.letterSpacing != nil || data.lineHeight != nil;
}

- (void)applyStylingToTypingAttrs:(NSMutableDictionary *)attributes {
  [self
      applyStylingForData:[self
                              getCustomStyleDataAt:self.host.textView
                                                       .selectedRange.location]
            toTypingAttrs:attributes];
}

- (void)applyStylingForData:(CustomStyleData *)data
              toTypingAttrs:(NSMutableDictionary *)attributes {
  if (data.fontSize != nil || data.fontFamily.length > 0) {
    UIFont *currentFont = attributes[NSFontAttributeName];
    attributes[NSFontAttributeName] = [self resolveFont:currentFont
                                                forData:data];
  }
  if (data.letterSpacing != nil) {
    attributes[NSKernAttributeName] = [self resolveKernForData:data];
  }
  if (data.lineHeight != nil) {
    attributes[NSParagraphStyleAttributeName] =
        [self resolveParagraphStyle:attributes[NSParagraphStyleAttributeName]
                            forData:data];
    attributes[LineBoxHeightAttributeName] =
        [self resolveLineBoxHeightForData:data];
  }
}

- (BOOL)styleCondition:(id)value range:(NSRange)range {
  if (![value isKindOfClass:[CustomStyleData class]])
    return NO;
  return ![(CustomStyleData *)value isEmpty];
}

- (void)applyStyling:(NSRange)range {
  if (range.length == 0)
    return;

  NSUInteger storageLength = self.host.textView.textStorage.length;
  if (storageLength == 0)
    return;

  NSRange safeRange = NSMakeRange(
      range.location, MIN(range.length, storageLength - range.location));

  // Enumerate each sub-range that carries its own CustomStyleData so that
  // characters with different data values each get the correct visual attrs.
  [self.host.textView.textStorage
      enumerateAttribute:CustomStyleAttributeName
                 inRange:safeRange
                 options:0
              usingBlock:^(id value, NSRange subRange, BOOL *stop) {
                if (![value isKindOfClass:[CustomStyleData class]])
                  return;
                CustomStyleData *data = (CustomStyleData *)value;
                if (data.isEmpty)
                  return;

                NSMutableDictionary *attrs = [NSMutableDictionary dictionary];
                if (data.foregroundColor != nil) {
                  attrs[NSForegroundColorAttributeName] = data.foregroundColor;
                  attrs[NSUnderlineColorAttributeName] = data.foregroundColor;
                  attrs[NSStrikethroughColorAttributeName] =
                      data.foregroundColor;
                }
                if (data.backgroundColor != nil) {
                  attrs[NSBackgroundColorAttributeName] =
                      [data.backgroundColor colorWithResolvedAlpha];
                }
                if (data.letterSpacing != nil) {
                  attrs[NSKernAttributeName] = [self resolveKernForData:data];
                }

                BOOL hasFontOverride =
                    data.fontSize != nil || data.fontFamily.length > 0;

                // Skip newline characters so background color doesn't bleed.
                NSArray *nonNewlineRanges =
                    [RangeUtils getNonNewlineRangesIn:self.host.textView
                                                range:subRange];
                for (NSValue *rangeVal in nonNewlineRanges) {
                  NSRange applyRange = [rangeVal rangeValue];
                  if (attrs.count > 0) {
                    [self.host.textView.textStorage addAttributes:attrs
                                                            range:applyRange];
                  }
                  if (data.letterSpacing != nil) {
                    [ZeroWidthSpaceUtils
                        applyKernForZeroWidthSpacesInRange:applyRange
                                                      host:self.host];
                  }
                  if (hasFontOverride) {
                    [self.host.textView.textStorage
                        enumerateAttribute:NSFontAttributeName
                                   inRange:applyRange
                                   options:0
                                usingBlock:^(id _Nullable value,
                                             NSRange fontRange,
                                             BOOL *_Nonnull stop) {
                                  UIFont *font = (UIFont *)value;
                                  UIFont *newFont = [self resolveFont:font
                                                              forData:data];
                                  [self.host.textView.textStorage
                                      addAttribute:NSFontAttributeName
                                             value:newFont
                                             range:fontRange];
                                }];
                  }
                }

                if (data.lineHeight != nil) {
                  [self.host.textView.textStorage
                      enumerateAttribute:NSParagraphStyleAttributeName
                                 inRange:subRange
                                 options:0
                              usingBlock:^(id _Nullable value,
                                           NSRange paragraphStyleRange,
                                           BOOL *_Nonnull stop) {
                                [self.host.textView.textStorage
                                    addAttribute:NSParagraphStyleAttributeName
                                           value:[self
                                                     resolveParagraphStyle:value
                                                                   forData:data]
                                           range:paragraphStyleRange];
                              }];
                  [self.host.textView.textStorage
                      addAttribute:LineBoxHeightAttributeName
                             value:[self resolveLineBoxHeightForData:data]
                             range:subRange];
                }
              }];
}

- (void)reapplyFromStylePair:(StylePair *)pair {
  NSRange range = [pair.rangeValue rangeValue];
  CustomStyleData *data = (CustomStyleData *)pair.styleValue;
  if (data == nil || data.isEmpty)
    return;
  [self.host.textView.textStorage addAttribute:CustomStyleAttributeName
                                         value:data
                                         range:range];
}

- (AttributeEntry *)getEntryIfPresent:(NSRange)range {
  CustomStyleData *data = [self getCustomStyleDataInRange:range];
  if (data == nil || data.isEmpty)
    return nullptr;

  AttributeEntry *entry = [[AttributeEntry alloc] init];
  entry.key = CustomStyleAttributeName;
  entry.value = data;
  return entry;
}

// MARK: - Public non-standard methods

- (void)setCustomStyleData:(CustomStyleData *)data
                     range:(NSRange)range
                withTyping:(BOOL)withTyping
            withDirtyRange:(BOOL)withDirtyRange {
  if (range.length > 0) {
    if (data == nil || data.isEmpty) {
      [self remove:range withDirtyRange:withDirtyRange];
      return;
    }
    [self.host.textView.textStorage addAttribute:CustomStyleAttributeName
                                           value:data
                                           range:range];
    if (withDirtyRange) {
      [self.host.attributesManager addDirtyRange:range];
    }
  }

  if (withTyping) {
    if (data == nil || data.isEmpty) {
      [self removeTyping];
    } else {
      NSMutableDictionary *newTypingAttrs =
          [self.host.textView.typingAttributes mutableCopy];
      newTypingAttrs[CustomStyleAttributeName] = data;
      self.host.textView.typingAttributes = newTypingAttrs;
    }
  }
}

- (CustomStyleData *_Nullable)getCustomStyleDataAt:(NSUInteger)location {
  NSRange selectedRange = self.host.textView.selectedRange;
  if (self.host.textView.isEditable && selectedRange.length == 0 &&
      selectedRange.location == location) {
    id typingValue =
        self.host.textView.typingAttributes[CustomStyleAttributeName];
    if ([typingValue isKindOfClass:[CustomStyleData class]])
      return (CustomStyleData *)typingValue;
    return nil;
  }

  return [self getStoredCustomStyleDataAt:location];
}

- (CustomStyleData *_Nullable)getCustomStyleDataInRange:(NSRange)range {
  if (range.length == 0)
    return [self getCustomStyleDataAt:range.location];

  NSMutableSet *foregroundColors = [NSMutableSet set];
  NSMutableSet *backgroundColors = [NSMutableSet set];
  NSMutableSet *fontSizes = [NSMutableSet set];
  NSMutableSet *fontFamilies = [NSMutableSet set];
  NSMutableSet *letterSpacings = [NSMutableSet set];
  NSMutableSet *lineHeights = [NSMutableSet set];

  [self.host.textView.textStorage
      enumerateAttribute:CustomStyleAttributeName
                 inRange:range
                 options:0
              usingBlock:^(id value, NSRange subRange, BOOL *stop) {
                CustomStyleData *data =
                    [value isKindOfClass:[CustomStyleData class]]
                        ? (CustomStyleData *)value
                        : nil;
                [foregroundColors
                    addObject:data.foregroundColor ?: (id)[NSNull null]];
                [backgroundColors
                    addObject:data.backgroundColor ?: (id)[NSNull null]];
                [fontSizes addObject:data.fontSize ?: (id)[NSNull null]];
                [fontFamilies addObject:data.fontFamily ?: (id)[NSNull null]];
                [letterSpacings
                    addObject:data.letterSpacing ?: (id)[NSNull null]];
                [lineHeights addObject:data.lineHeight ?: (id)[NSNull null]];
              }];

  CustomStyleData *data = [[CustomStyleData alloc] init];
  data.foregroundColor = [self getSingleValueFromSet:foregroundColors];
  data.backgroundColor = [self getSingleValueFromSet:backgroundColors];
  data.fontSize = [self getSingleValueFromSet:fontSizes];
  data.fontFamily = [self getSingleValueFromSet:fontFamilies];
  data.letterSpacing = [self getSingleValueFromSet:letterSpacings];
  data.lineHeight = [self getSingleValueFromSet:lineHeights];
  if (data.isEmpty)
    return nil;

  return data;
}

// Reads CustomStyleData directly from textStorage, bypassing typingAttributes.
- (CustomStyleData *_Nullable)getStoredCustomStyleDataAt:(NSUInteger)location {
  NSUInteger length = self.host.textView.textStorage.length;
  if (length == 0)
    return nil;
  NSUInteger searchLocation = (location >= length) ? length - 1 : location;
  id value = [self.host.textView.textStorage attribute:CustomStyleAttributeName
                                               atIndex:searchLocation
                                 longestEffectiveRange:nil
                                               inRange:NSMakeRange(0, length)];
  if (![value isKindOfClass:[CustomStyleData class]])
    return nil;
  return (CustomStyleData *)value;
}

- (void)applyStyleFromDict:(NSDictionary *)dict selectedRange:(NSRange)range {
  BOOL withTyping = range.length == 0;

  if (!withTyping) {
    // Enumerate each existing sub-range and merge the partial update into its
    // own data so per-character differences (e.g. fg color on some chars) are
    // preserved when only one field (e.g. bg color) is being changed.
    NSUInteger storageLength = self.host.textView.textStorage.length;
    if (storageLength == 0)
      return;

    NSRange safeRange = NSMakeRange(
        range.location, MIN(range.length, storageLength - range.location));

    [self.host.textView.textStorage
        enumerateAttribute:CustomStyleAttributeName
                   inRange:safeRange
                   options:0
                usingBlock:^(id value, NSRange subRange, BOOL *stop) {
                  CustomStyleData *existing =
                      [value isKindOfClass:[CustomStyleData class]]
                          ? (CustomStyleData *)value
                          : nil;
                  CustomStyleData *merged =
                      existing != nil ? [existing copy]
                                      : [[CustomStyleData alloc] init];
                  [merged mergeFromDict:dict];
                  [self setCustomStyleData:merged
                                     range:subRange
                                withTyping:NO
                            withDirtyRange:YES];
                }];
  } else {
    // Cursor only: merge into current data and update typing attributes.
    CustomStyleData *existing = [self getCustomStyleDataAt:range.location];
    CustomStyleData *merged =
        existing != nil ? [existing copy] : [[CustomStyleData alloc] init];
    [merged mergeFromDict:dict];
    [self setCustomStyleData:merged
                       range:range
                  withTyping:YES
              withDirtyRange:NO];
    [self.host.attributesManager
        didRemoveTypingAttribute:CustomStyleAttributeName];
  }
}

- (id _Nullable)getSingleValueFromSet:(NSSet *)values {
  if (values.count != 1)
    return nil;
  id value = values.anyObject;
  return value == [NSNull null] ? nil : value;
}

@end
