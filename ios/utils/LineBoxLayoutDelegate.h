#pragma once
#import <UIKit/UIKit.h>

extern NSString *const LineBoxHeightAttributeName;

@interface LineBoxLayoutDelegate : NSObject <NSLayoutManagerDelegate>

+ (instancetype)shared;

+ (CGFloat)usedHeightOfAttributedString:(NSAttributedString *)string
                                  width:(CGFloat)width
                   maximumNumberOfLines:(NSUInteger)maximumNumberOfLines
                          lineBreakMode:(NSLineBreakMode)lineBreakMode;

@end
