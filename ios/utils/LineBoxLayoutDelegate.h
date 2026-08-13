#pragma once
#import <UIKit/UIKit.h>

// Places every line the way a CSS line box does. Stateless, so one shared
// instance serves every text view; `NSLayoutManager.delegate` is weak.
@interface LineBoxLayoutDelegate : NSObject <NSLayoutManagerDelegate>

+ (instancetype)shared;

@end
