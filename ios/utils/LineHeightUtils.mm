#import "LineHeightUtils.h"

@implementation LineHeightUtils

+ (void)applyLineHeight:(CGFloat)lineHeight
       toParagraphStyle:(NSMutableParagraphStyle *)paragraphStyle {
  if (paragraphStyle == nil) {
    return;
  }

  paragraphStyle.minimumLineHeight = lineHeight;
  // CSS line-height is the exact line box height, so a font whose natural
  // height exceeds it must overflow the box rather than grow it.
  paragraphStyle.maximumLineHeight = lineHeight;
}

// Vertical placement is decided per line by LineBoxLayoutDelegate, which moves
// the line's shared baseline. A per-run NSBaselineOffsetAttributeName would
// stack on top of that and shift runs against each other, so it is stripped
// wherever this used to write one.
+ (void)applyBaselineOffsetToAttributes:(NSMutableDictionary *)attributes {
  [attributes removeObjectForKey:NSBaselineOffsetAttributeName];
}

+ (void)applyBaselineOffsetsInTextStorage:(NSTextStorage *)textStorage
                                    range:(NSRange)range {
  if (textStorage == nil || range.length == 0) {
    return;
  }

  [textStorage removeAttribute:NSBaselineOffsetAttributeName range:range];
}

@end
