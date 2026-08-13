#import "ColorExtension.h"
#import "EnrichedTextInputView.h"
#import "StyleHeaders.h"

@implementation ForegroundColorStyle

+ (StyleType)getType {
  return ForegroundColor;
}

- (NSString *)getKey {
  return @"EnrichedForegroundColor";
}

- (void)applyStyling:(NSRange)range {
  NSString *value = [self getValueAt:range.location];
  if (value == nullptr) {
    return;
  }
  UIColor *color = [UIColor colorFromHexString:value];
  if (color == nullptr) {
    return;
  }

  [self.host.textView.textStorage addAttribute:NSForegroundColorAttributeName
                                         value:color
                                         range:range];
}

- (void)applyStylingToTypingAttrs:(NSMutableDictionary *)attributes {
  NSString *value = attributes[[self getKey]];
  if (value == nullptr) {
    return;
  }
  UIColor *color = [UIColor colorFromHexString:value];
  if (color == nullptr) {
    return;
  }
  attributes[NSForegroundColorAttributeName] = color;
}

@end
