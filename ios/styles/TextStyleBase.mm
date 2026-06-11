#import "AttributeEntry.h"
#import "EnrichedTextInputView.h"
#import "StyleHeaders.h"

@implementation TextStyleBase

- (BOOL)isParagraph {
  return NO;
}

// Any non-null value means the style is active - the attribute's value is the
// parametrized style value, not a constant marker.
- (BOOL)styleCondition:(id)value range:(NSRange)range {
  return value != nullptr;
}

- (void)setValue:(NSString *)value range:(NSRange)range {
  if (range.length >= 1) {
    if (value != nullptr) {
      [self add:range withValue:value withTyping:YES withDirtyRange:YES];
    } else {
      [self remove:range withDirtyRange:YES];
    }
  } else {
    if (value != nullptr) {
      [self addTypingWithValue:value];
    } else {
      [self removeTyping];
    }
  }
}

- (NSString *)getValueAt:(NSUInteger)location {
  if (location >= self.host.textView.textStorage.string.length) {
    return nullptr;
  }
  return [self.host.textView.textStorage attribute:[self getKey]
                                           atIndex:location
                                    effectiveRange:nullptr];
}

- (NSString *)getActiveValue {
  NSRange selectedRange = self.host.textView.selectedRange;
  if (selectedRange.length == 0) {
    return self.host.textView.typingAttributes[[self getKey]];
  }
  return [self getValueAt:selectedRange.location];
}

- (CGFloat)scaledValue:(NSString *)value {
  CGFloat rawValue = [value floatValue];
  if (![self.host.config allowFontScaling]) {
    return rawValue;
  }
  return [[UIFontMetrics defaultMetrics] scaledValueForValue:rawValue];
}

// Extend the typing attributes with the actual value of the previous
// character, so typing continues the style with the same value.
- (AttributeEntry *)getEntryIfPresent:(NSRange)range {
  NSString *value = [self getValueAt:range.location];
  if (value == nullptr) {
    return nullptr;
  }

  AttributeEntry *entry = [[AttributeEntry alloc] init];
  entry.key = [self getKey];
  entry.value = value;
  return entry;
}

// Restore the marker attribute together with its value
- (void)reapplyFromStylePair:(StylePair *)pair {
  NSString *value = (NSString *)pair.styleValue;
  if (value == nullptr) {
    return;
  }
  [self add:[pair.rangeValue rangeValue]
           withValue:value
          withTyping:NO
      withDirtyRange:NO];
}

- (BOOL)appliesStylingToTyping {
  return YES;
}

@end
