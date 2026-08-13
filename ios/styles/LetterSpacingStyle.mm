#import "EnrichedTextInputView.h"
#import "StyleHeaders.h"

@implementation LetterSpacingStyle

+ (StyleType)getType {
  return LetterSpacing;
}

- (NSString *)getKey {
  return @"EnrichedLetterSpacing";
}

- (void)applyStyling:(NSRange)range {
  NSString *value = [self getValueAt:range.location];
  if (value == nullptr) {
    return;
  }

  [self.host.textView.textStorage addAttribute:NSKernAttributeName
                                         value:@([self scaledValue:value])
                                         range:range];
}

- (void)applyStylingToTypingAttrs:(NSMutableDictionary *)attributes {
  NSString *value = attributes[[self getKey]];
  if (value == nullptr) {
    return;
  }
  attributes[NSKernAttributeName] = @([self scaledValue:value]);
}

@end
