#import <UIKit/UIKit.h>
#pragma once

@interface UIColor (ColorExtension)
- (BOOL)isEqualToColor:(UIColor *)otherColor;
- (UIColor *)colorWithAlphaIfNotTransparent:(CGFloat)newAlpha;
// Parses "#RGB", "#RRGGBB" and "#RRGGBBAA" strings; returns nil for anything
// else.
+ (UIColor *)colorFromHexString:(NSString *)hexString;
// Serializes the color back to a "#RRGGBB" (or "#RRGGBBAA" when not fully
// opaque) string.
- (NSString *)toHexString;
@end
