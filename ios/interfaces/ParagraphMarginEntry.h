#pragma once
#import <Foundation/Foundation.h>

@interface ParagraphMarginEntry : NSObject
@property(nonatomic) NSRange range;
@property(nonatomic, nullable) NSNumber *marginTop;
@property(nonatomic, nullable) NSNumber *marginBottom;
@end
