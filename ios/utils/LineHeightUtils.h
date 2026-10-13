#pragma once
#import <UIKit/UIKit.h>

// A span's resolved (scaled) line height as an NSNumber. TextKit keeps a single
// paragraph style per paragraph (the first character's), so a span's line
// height stored only there is lost for every later span of the paragraph.
extern NSString *const LineBoxHeightAttributeName;

@interface LineHeightUtils : NSObject
+ (void)applyLineHeight:(CGFloat)lineHeight
       toParagraphStyle:(NSMutableParagraphStyle *)paragraphStyle;
+ (void)applyBaselineOffsetToAttributes:(NSMutableDictionary *)attributes;
+ (void)applyBaselineOffsetsInTextStorage:(NSTextStorage *)textStorage
                                    range:(NSRange)range;
@end
