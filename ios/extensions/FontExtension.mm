#import "FontExtension.h"
#import <React/RCTLog.h>

@implementation UIFont (FontExtension)

- (BOOL)isBold {
  return (self.fontDescriptor.symbolicTraits & UIFontDescriptorTraitBold) ==
         UIFontDescriptorTraitBold;
}

- (UIFont *)setBold {
  if ([self isBold]) {
    return self;
  }
  UIFontDescriptorSymbolicTraits newTraits =
      (self.fontDescriptor.symbolicTraits | UIFontDescriptorTraitBold);
  UIFontDescriptor *fontDescriptor =
      [self.fontDescriptor fontDescriptorWithSymbolicTraits:newTraits];
  if (fontDescriptor != nullptr) {
    return [UIFont fontWithDescriptor:fontDescriptor size:0];
  } else {
    RCTLogWarn(@"[EnrichedTextInput]: Couldn't apply bold trait to the font.");
    return self;
  }
}

- (BOOL)isItalic {
  return (self.fontDescriptor.symbolicTraits & UIFontDescriptorTraitItalic) ==
         UIFontDescriptorTraitItalic;
}

- (UIFont *)setItalic {
  if ([self isItalic]) {
    return self;
  }
  UIFontDescriptorSymbolicTraits newTraits =
      (self.fontDescriptor.symbolicTraits | UIFontDescriptorTraitItalic);
  UIFontDescriptor *fontDescriptor =
      [self.fontDescriptor fontDescriptorWithSymbolicTraits:newTraits];
  if (fontDescriptor != nullptr) {
    return [UIFont fontWithDescriptor:fontDescriptor size:0];
  } else {
    RCTLogWarn(
        @"[EnrichedTextInput]: Couldn't apply italic trait to the font.");
    return self;
  }
}

- (UIFont *)withFontTraits:(UIFont *)from {
  UIFont *newFont = self;
  if ([from isBold]) {
    newFont = [newFont setBold];
  }
  if ([from isItalic]) {
    newFont = [newFont setItalic];
  }
  return newFont;
}

- (UIFont *)setSize:(CGFloat)size {
  UIFontDescriptor *newFontDescriptor =
      [self.fontDescriptor fontDescriptorWithSize:size];
  if (newFontDescriptor != nullptr) {
    return [UIFont fontWithDescriptor:newFontDescriptor size:0];
  } else {
    RCTLogWarn(
        @"[EnrichedTextInput]: Couldn't apply heading style to the font.");
    return self;
  }
}

- (UIFont *)setFamily:(NSString *)family {
  // Try a full font name first (e.g. "Helvetica-Bold")...
  UIFont *baseFont = [UIFont fontWithName:family size:self.pointSize];
  if (baseFont == nullptr) {
    // ...then resolve the family through fontNamesForFamilyName:. This is the
    // lookup expo-font swizzles for dynamically loaded fonts, so font family
    // aliases registered at runtime resolve here. Prefer the shortest face
    // name - regular faces have no style suffix - and let withFontTraits:
    // restore bold/italic afterwards.
    NSArray<NSString *> *faceNames = [UIFont fontNamesForFamilyName:family];
    NSString *bestFaceName = nullptr;
    for (NSString *faceName in faceNames) {
      if (bestFaceName == nullptr || faceName.length < bestFaceName.length) {
        bestFaceName = faceName;
      }
    }
    if (bestFaceName != nullptr) {
      baseFont = [UIFont fontWithName:bestFaceName size:self.pointSize];
    }
  }
  if (baseFont == nullptr) {
    // ...lastly fall back to descriptor matching by the family attribute
    UIFontDescriptor *descriptor =
        [UIFontDescriptor fontDescriptorWithFontAttributes:@{
          UIFontDescriptorFamilyAttribute : family
        }];
    baseFont = [UIFont fontWithDescriptor:descriptor size:self.pointSize];
  }
  if (baseFont == nullptr) {
    RCTLogWarn(@"[EnrichedTextInput]: Couldn't resolve font family: %@.",
               family);
    return self;
  }
  // preserve the bold/italic traits of the original font
  return [baseFont withFontTraits:self];
}

@end
