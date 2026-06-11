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
    // ...then fall back to a font family name (e.g. "Helvetica")
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
