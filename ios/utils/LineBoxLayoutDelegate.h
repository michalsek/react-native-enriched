#pragma once
#import <UIKit/UIKit.h>

// Places every line the way a CSS line box does. Stateless, so one shared
// instance serves every text view; `NSLayoutManager.delegate` is weak.
@interface LineBoxLayoutDelegate : NSObject <NSLayoutManagerDelegate>

+ (instancetype)shared;

// Height of the string laid out the way the text views lay it out, so a
// measurement matches what they draw.
+ (CGFloat)usedHeightOfAttributedString:(NSAttributedString *)string
                                  width:(CGFloat)width
                   maximumNumberOfLines:(NSUInteger)maximumNumberOfLines
                          lineBreakMode:(NSLineBreakMode)lineBreakMode;

@end
