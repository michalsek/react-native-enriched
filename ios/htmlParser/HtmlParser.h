#pragma once
#import "EnrichedViewHost.h"
#import <UIKit/UIKit.h>

@interface HtmlParser : NSObject
+ (NSString *_Nullable)initiallyProcessHtml:(NSString *_Nonnull)html
                          useHtmlNormalizer:(BOOL)useHtmlNormalizer;
+ (NSArray *_Nonnull)getTextAndStylesFromHtml:(NSString *_Nonnull)fixedHtml;
+ (NSString *_Nonnull)parseToHtmlFromRange:(NSRange)range
                                      host:(id<EnrichedViewHost>)host;
// Checks whether the style type is one of the inline text styles (font
// family, font size, letter spacing, line height).
+ (BOOL)isTextStyleType:(NSNumber *_Nonnull)style;
@end
