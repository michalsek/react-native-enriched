#import "ColorExtension.h"

@implementation UIColor (ColorExtension)
- (BOOL)isEqualToColor:(UIColor *)otherColor {
  CGColorSpaceRef colorSpaceRGB = CGColorSpaceCreateDeviceRGB();

  UIColor * (^convertColorToRGBSpace)(UIColor *) = ^(UIColor *color) {
    if (CGColorSpaceGetModel(CGColorGetColorSpace(color.CGColor)) ==
        kCGColorSpaceModelMonochrome) {
      const CGFloat *oldComponents = CGColorGetComponents(color.CGColor);
      CGFloat components[4] = {oldComponents[0], oldComponents[0],
                               oldComponents[0], oldComponents[1]};
      CGColorRef colorRef = CGColorCreate(colorSpaceRGB, components);

      UIColor *color = [UIColor colorWithCGColor:colorRef];
      CGColorRelease(colorRef);
      return color;
    } else {
      return color;
    }
  };

  UIColor *selfColor = convertColorToRGBSpace(self);
  otherColor = convertColorToRGBSpace(otherColor);
  CGColorSpaceRelease(colorSpaceRGB);

  return [selfColor isEqual:otherColor];
}

- (UIColor *)colorWithAlphaIfNotTransparent:(CGFloat)newAlpha {
  CGFloat alpha = 0.0;
  [self getRed:nil green:nil blue:nil alpha:&alpha];
  if (alpha > 0.0) {
    return [self colorWithAlphaComponent:newAlpha];
  }
  return self;
}

+ (UIColor *)colorFromHexString:(NSString *)hexString {
  NSString *hex = [hexString
      stringByTrimmingCharactersInSet:[NSCharacterSet
                                          whitespaceAndNewlineCharacterSet]];
  if (![hex hasPrefix:@"#"]) {
    return nil;
  }
  hex = [hex substringFromIndex:1];

  // expand the #RGB shorthand to #RRGGBB
  if (hex.length == 3) {
    NSMutableString *expanded = [[NSMutableString alloc] init];
    for (NSUInteger i = 0; i < hex.length; i++) {
      NSString *character = [hex substringWithRange:NSMakeRange(i, 1)];
      [expanded appendString:character];
      [expanded appendString:character];
    }
    hex = expanded;
  }

  if (hex.length != 6 && hex.length != 8) {
    return nil;
  }

  unsigned long long value = 0;
  NSScanner *scanner = [NSScanner scannerWithString:hex];
  if (![scanner scanHexLongLong:&value] || !scanner.isAtEnd) {
    return nil;
  }

  CGFloat alpha = 1.0;
  if (hex.length == 8) {
    alpha = (CGFloat)(value & 0xFF) / 255.0;
    value >>= 8;
  }

  CGFloat red = (CGFloat)((value >> 16) & 0xFF) / 255.0;
  CGFloat green = (CGFloat)((value >> 8) & 0xFF) / 255.0;
  CGFloat blue = (CGFloat)(value & 0xFF) / 255.0;

  return [UIColor colorWithRed:red green:green blue:blue alpha:alpha];
}

- (NSString *)toHexString {
  CGFloat red = 0.0, green = 0.0, blue = 0.0, alpha = 1.0;
  if (![self getRed:&red green:&green blue:&blue alpha:&alpha]) {
    return nil;
  }

  int redInt = (int)lround(red * 255.0);
  int greenInt = (int)lround(green * 255.0);
  int blueInt = (int)lround(blue * 255.0);
  int alphaInt = (int)lround(alpha * 255.0);

  if (alphaInt < 255) {
    return [NSString stringWithFormat:@"#%02X%02X%02X%02X", redInt, greenInt,
                                      blueInt, alphaInt];
  }
  return
      [NSString stringWithFormat:@"#%02X%02X%02X", redInt, greenInt, blueInt];
}
@end
