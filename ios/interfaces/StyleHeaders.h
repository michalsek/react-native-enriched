#pragma once
#import "ImageData.h"
#import "LinkData.h"
#import "MentionParams.h"
#import "StyleBase.h"

@interface BoldStyle : StyleBase
@end

@interface ItalicStyle : StyleBase
@end

@interface UnderlineStyle : StyleBase
@end

@interface StrikethroughStyle : StyleBase
@end

@interface InlineCodeStyle : StyleBase
@end

@interface LinkStyle : StyleBase
- (void)addLink:(LinkData *)linkData
            range:(NSRange)range
    withSelection:(BOOL)withSelection;
- (LinkData *)getLinkDataAt:(NSUInteger)location;
- (NSRange)getFullLinkRangeAt:(NSUInteger)location;
- (void)handleAutomaticLinks:(NSString *)word inRange:(NSRange)wordRange;
- (void)handleManualLinks:(NSString *)word inRange:(NSRange)wordRange;
- (void)applyLinkMetaWithData:(LinkData *)linkData range:(NSRange)range;
@end

@interface MentionStyle : StyleBase
- (void)addMention:(NSString *)indicator
              text:(NSString *)text
        attributes:(NSString *)attributes;
- (void)addMentionAtRange:(NSRange)range params:(MentionParams *)params;
- (void)startMentionWithIndicator:(NSString *)indicator;
- (void)handleExistingMentions;
- (void)manageMentionEditing;
- (MentionParams *)getMentionParamsAt:(NSUInteger)location;
- (NSRange)getFullMentionRangeAt:(NSUInteger)location;
- (NSValue *)getActiveMentionRange;
- (void)applyMentionMeta:(MentionParams *)params range:(NSRange)range;
@end

@interface HeadingStyleBase : StyleBase
- (CGFloat)getHeadingFontSize;
- (BOOL)isHeadingBold;
- (BOOL)handleNewlinesInRange:(NSRange)range replacementText:(NSString *)text;
@end

@interface H1Style : HeadingStyleBase
@end

@interface H2Style : HeadingStyleBase
@end

@interface H3Style : HeadingStyleBase
@end

@interface H4Style : HeadingStyleBase
@end

@interface H5Style : HeadingStyleBase
@end

@interface H6Style : HeadingStyleBase
@end

@interface UnorderedListStyle : StyleBase
- (BOOL)tryHandlingListShorcutInRange:(NSRange)range
                      replacementText:(NSString *)text;
@end

@interface OrderedListStyle : StyleBase
- (BOOL)tryHandlingListShorcutInRange:(NSRange)range
                      replacementText:(NSString *)text;
@end

@interface CheckboxListStyle : StyleBase
- (void)toggleWithChecked:(BOOL)checked range:(NSRange)range;
- (void)addWithChecked:(BOOL)checked
                 range:(NSRange)range
            withTyping:(BOOL)withTyping
        withDirtyRange:(BOOL)withDirtyRange;
- (void)toggleCheckedAt:(NSUInteger)location
         withDirtyRange:(BOOL)withDirtyRange;
- (BOOL)getCheckboxStateAt:(NSUInteger)location;
- (BOOL)handleNewlinesInRange:(NSRange)range replacementText:(NSString *)text;
@end

@interface AlignmentStyle : StyleBase
- (void)addAlignment:(NSTextAlignment)alignment
               range:(NSRange)range
          withTyping:(BOOL)withTyping
      withDirtyRange:(BOOL)withDirtyRange;
- (NSString *)getStyleState;
@end

@interface ParagraphMarginStyle : StyleBase
- (void)addMarginTop:(NSNumber *)marginTop
        marginBottom:(NSNumber *)marginBottom
               range:(NSRange)range
          withTyping:(BOOL)withTyping
      withDirtyRange:(BOOL)withDirtyRange;
@end

@interface BlockQuoteStyle : StyleBase
@end

@interface CodeBlockStyle : StyleBase
@end

@interface ImageStyle : StyleBase
- (void)addImage:(NSString *)uri width:(CGFloat)width height:(CGFloat)height;
- (void)addImageAtRange:(NSRange)range
              imageData:(ImageData *)imageData
          withSelection:(BOOL)withSelection
         withDirtyRange:(BOOL)withDirtyRange;
- (ImageData *)getImageDataAt:(NSUInteger)location;
@end

// Base class for the inline text styles (font family, font size, letter
// spacing, line height). The style value (always kept as a string) is stored
// as the custom attribute's value.
@interface TextStyleBase : StyleBase
// Applies the value to the range (or to the typing attributes when the range
// is empty). Passing nullptr removes the style.
- (void)setValue:(NSString *)value range:(NSRange)range;
// Returns the style value at the given text location or nullptr.
- (NSString *)getValueAt:(NSUInteger)location;
// Returns the style value at the current selection (taking the typing
// attributes into account) or nullptr.
- (NSString *)getActiveValue;
// Converts the value to a number respecting the allowFontScaling setting.
- (CGFloat)scaledValue:(NSString *)value;
@end

@interface FontFamilyStyle : TextStyleBase
@end

@interface FontSizeStyle : TextStyleBase
@end

@interface LetterSpacingStyle : TextStyleBase
@end

@interface LineHeightStyle : TextStyleBase
@end

@interface ForegroundColorStyle : TextStyleBase
@end
