#pragma once
#import <UIKit/UIKit.h>

@class EnrichedTextInputView;
@class CustomStyleData;

@interface InputAttributesManager : NSObject
@property(nonatomic, weak) EnrichedTextInputView *input;
@property(nonatomic, copy) NSDictionary *trailingParagraphAttributes;
- (instancetype)initWithInput:(EnrichedTextInputView *)input;
- (void)addDirtyRange:(NSRange)range;
- (NSArray *)getDirtyRanges;
- (void)shiftDirtyRangesWithEditedRange:(NSRange)editedRange
                         changeInLength:(NSInteger)delta;
- (void)didRemoveTypingAttribute:(NSString *)key;
- (void)clearRemovedTypingAttributes;
- (void)manageTypingAttributesWithOnlySelection:(BOOL)onlySelectionChanged;
- (void)repeatRecentTypingAttributesManagement;
- (void)handleDirtyRangesStyling;
- (NSSet<NSString *> *)customAttributesKeys;
- (NSDictionary *)typingAttributesFromParagraphAttributes:
    (NSDictionary *)paragraphAttributes;
- (NSDictionary *)paragraphAttributesFromParsedParagraph:
    (NSDictionary *)parsedParagraph;
- (CustomStyleData *)customStyleDataAt:(NSUInteger)location;
- (void)storeTypingTextStylesOnEmptyParagraphAt:(NSUInteger)location;
- (void)storeTrailingParagraphAttributesIfNeeded;
@end
