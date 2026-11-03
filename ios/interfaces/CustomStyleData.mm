#import "CustomStyleData.h"

@implementation CustomStyleData

- (BOOL)isEmpty {
  return _foregroundColor == nil && _backgroundColor == nil &&
         _fontSize == nil && _fontFamily == nil && _letterSpacing == nil &&
         _lineHeight == nil;
}

- (void)mergeFromDict:(NSDictionary *)dict {
  id fgVal = dict[@"foregroundColor"];
  if (fgVal != nil) {
    self.foregroundColor =
        [fgVal isKindOfClass:[UIColor class]] ? (UIColor *)fgVal : nil;
  }
  id bgVal = dict[@"backgroundColor"];
  if (bgVal != nil) {
    self.backgroundColor =
        [bgVal isKindOfClass:[UIColor class]] ? (UIColor *)bgVal : nil;
  }
  id fsVal = dict[@"fontSize"];
  if (fsVal != nil) {
    if ([fsVal isKindOfClass:[NSNumber class]] &&
        [(NSNumber *)fsVal doubleValue] > 0) {
      self.fontSize = (NSNumber *)fsVal;
    } else {
      self.fontSize = nil;
    }
  }
  id ffVal = dict[@"fontFamily"];
  if (ffVal != nil) {
    self.fontFamily =
        [ffVal isKindOfClass:[NSString class]] ? (NSString *)ffVal : nil;
  }
  id lsVal = dict[@"letterSpacing"];
  if (lsVal != nil) {
    self.letterSpacing = [lsVal isKindOfClass:[NSNumber class]] &&
                                 [(NSNumber *)lsVal doubleValue] != 0
                             ? (NSNumber *)lsVal
                             : nil;
  }
  id lhVal = dict[@"lineHeight"];
  if (lhVal != nil) {
    self.lineHeight = [lhVal isKindOfClass:[NSNumber class]] &&
                              [(NSNumber *)lhVal doubleValue] > 0
                          ? (NSNumber *)lhVal
                          : nil;
  }
}

- (BOOL)isEqual:(id)object {
  if (self == object)
    return YES;
  if (![object isKindOfClass:[CustomStyleData class]])
    return NO;
  CustomStyleData *other = (CustomStyleData *)object;
  BOOL fgEqual = (_foregroundColor == other.foregroundColor) ||
                 [_foregroundColor isEqual:other.foregroundColor];
  BOOL bgEqual = (_backgroundColor == other.backgroundColor) ||
                 [_backgroundColor isEqual:other.backgroundColor];
  BOOL fsEqual = (_fontSize == other.fontSize) ||
                 (_fontSize != nil && other.fontSize != nil &&
                  [_fontSize isEqualToNumber:other.fontSize]);
  BOOL ffEqual = (_fontFamily == other.fontFamily) ||
                 [_fontFamily isEqualToString:other.fontFamily];
  BOOL lsEqual = (_letterSpacing == other.letterSpacing) ||
                 (_letterSpacing != nil && other.letterSpacing != nil &&
                  [_letterSpacing isEqualToNumber:other.letterSpacing]);
  BOOL lhEqual = (_lineHeight == other.lineHeight) ||
                 (_lineHeight != nil && other.lineHeight != nil &&
                  [_lineHeight isEqualToNumber:other.lineHeight]);
  return fgEqual && bgEqual && fsEqual && ffEqual && lsEqual && lhEqual;
}

- (NSUInteger)hash {
  return [_foregroundColor hash] ^ [_backgroundColor hash] ^ [_fontSize hash] ^
         [_fontFamily hash] ^ [_letterSpacing hash] ^ [_lineHeight hash];
}

- (id)copyWithZone:(NSZone *)zone {
  CustomStyleData *copy = [[CustomStyleData allocWithZone:zone] init];
  copy.foregroundColor = self.foregroundColor;
  copy.backgroundColor = self.backgroundColor;
  copy.fontSize = self.fontSize;
  copy.fontFamily = self.fontFamily;
  copy.letterSpacing = self.letterSpacing;
  copy.lineHeight = self.lineHeight;
  return copy;
}

@end
