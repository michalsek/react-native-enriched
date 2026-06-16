#pragma once
#import <UIKit/UIKit.h>

@interface LineHeightUtils : NSObject
+ (void)applyLineHeight:(CGFloat)lineHeight
       toParagraphStyle:(NSMutableParagraphStyle *)paragraphStyle;
+ (void)applyBaselineOffsetToAttributes:(NSMutableDictionary *)attributes;
+ (void)applyBaselineOffsetsInTextStorage:(NSTextStorage *)textStorage
                                    range:(NSRange)range;
@end
