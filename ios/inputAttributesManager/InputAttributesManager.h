#pragma once
#import <UIKit/UIKit.h>

@class EnrichedTextInputView;

@interface InputAttributesManager : NSObject
@property(nonatomic, weak) EnrichedTextInputView *input;
// Styles of the empty last paragraph, which has no character to carry them.
// Keyed like text attributes: inline text style keys, the paragraph margin key
// and NSParagraphStyleAttributeName (alignment and margins).
@property(nonatomic, copy) NSDictionary *trailingParagraphAttributes;
- (instancetype)initWithInput:(EnrichedTextInputView *)input;
- (void)addDirtyRange:(NSRange)range;
- (NSArray *)getDirtyRanges;
- (void)shiftDirtyRangesWithEditedRange:(NSRange)editedRange
                         changeInLength:(NSInteger)delta;
- (void)didRemoveTypingAttribute:(NSString *)key;
- (void)clearRemovedTypingAttributes;
- (void)manageTypingAttributesWithOnlySelection:(BOOL)onlySelectionChanged;
- (void)handleDirtyRangesStyling;
- (NSSet<NSString *> *)customAttributesKeys;
- (NSDictionary *)typingAttributesFromParagraphAttributes:
    (NSDictionary *)paragraphAttributes;
- (NSDictionary *)paragraphAttributesFromParsedParagraph:
    (NSDictionary *)parsedParagraph;
- (NSDictionary *)textStyleValuesAt:(NSUInteger)location;
- (void)storeTypingTextStylesOnEmptyParagraphAt:(NSUInteger)location;
- (void)storeTrailingParagraphAttributesIfNeeded;
@end
