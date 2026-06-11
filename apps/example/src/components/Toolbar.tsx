import {
  FlatList,
  PanResponder,
  Pressable,
  StyleSheet,
  Text,
  View,
  type ListRenderItemInfo,
} from 'react-native';
import { ToolbarButton } from './ToolbarButton';
import type {
  OnChangeStateEvent,
  EnrichedTextInputInstance,
} from 'react-native-enriched';
import { useMemo, useState, type FC } from 'react';

const GRID_COLUMNS = 8;

const STYLE_ITEMS = [
  {
    name: 'bold',
    icon: 'bold',
  },
  {
    name: 'italic',
    icon: 'italic',
  },
  {
    name: 'underline',
    icon: 'underline',
  },
  {
    name: 'strikethrough',
    icon: 'strikethrough',
  },
  {
    name: 'inline-code',
    icon: 'code',
  },
  {
    name: 'heading-1',
    text: 'H1',
  },
  {
    name: 'heading-2',
    text: 'H2',
  },
  {
    name: 'heading-3',
    text: 'H3',
  },
  {
    name: 'heading-4',
    text: 'H4',
  },
  {
    name: 'heading-5',
    text: 'H5',
  },
  {
    name: 'heading-6',
    text: 'H6',
  },
  {
    name: 'quote',
    icon: 'quote-right',
  },
  {
    name: 'code-block',
    icon: 'file-code-o',
  },
  {
    name: 'image',
    icon: 'image',
  },
  {
    name: 'link',
    icon: 'link',
  },
  {
    name: 'mention',
    icon: 'at',
  },
  {
    name: 'unordered-list',
    icon: 'list-ul',
  },
  {
    name: 'ordered-list',
    icon: 'list-ol',
  },
  {
    name: 'checkbox-list',
    icon: 'check-square-o',
  },
  {
    name: 'align-left',
    icon: 'align-left',
  },
  {
    name: 'align-center',
    icon: 'align-center',
  },
  {
    name: 'align-right',
    icon: 'align-right',
  },
] as const;

const FONT_OPTIONS = [
  {
    label: 'Default',
    value: null,
    testID: 'toolbar-font-family-default',
  },
  {
    label: 'Nunito',
    value: 'Nunito-Regular',
    testID: 'toolbar-font-family-nunito',
  },
  {
    label: 'Cascadia',
    value: 'CascadiaCode-Regular',
    testID: 'toolbar-font-family-cascadia',
  },
] as const;

const FONT_SIZE_FALLBACK = 18;
const LETTER_SPACING_FALLBACK = 0;
const LINE_HEIGHT_FALLBACK = 24;

type Item = (typeof STYLE_ITEMS)[number];
type StylesState = OnChangeStateEvent;

interface TextStyleSliderProps {
  label: string;
  value: number;
  valueLabel: string;
  min: number;
  max: number;
  step: number;
  isActive: boolean;
  isDisabled: boolean;
  onChange: (value: number) => void;
  onReset: () => void;
  testID: string;
  resetTestID: string;
}

const clamp = (value: number, min: number, max: number) => {
  return Math.min(Math.max(value, min), max);
};

const decimalPlaces = (value: number) => {
  const [, decimals = ''] = value.toString().split('.');
  return decimals.length;
};

const snapToStep = (value: number, min: number, step: number) => {
  const snapped = Math.round((value - min) / step) * step + min;
  return Number(snapped.toFixed(decimalPlaces(step)));
};

const formatValue = (value: number, step: number) => {
  return value.toFixed(decimalPlaces(step));
};

const TextStyleSlider: FC<TextStyleSliderProps> = ({
  label,
  value,
  valueLabel,
  min,
  max,
  step,
  isActive,
  isDisabled,
  onChange,
  onReset,
  testID,
  resetTestID,
}) => {
  const [trackWidth, setTrackWidth] = useState(0);
  const progress = clamp((value - min) / (max - min), 0, 1);

  const panResponder = useMemo(
    () =>
      PanResponder.create({
        onStartShouldSetPanResponder: () => !isDisabled,
        onStartShouldSetPanResponderCapture: () => !isDisabled,
        onMoveShouldSetPanResponder: () => !isDisabled,
        onMoveShouldSetPanResponderCapture: () => !isDisabled,
        onPanResponderGrant: (event) => {
          if (trackWidth <= 0) return;

          const ratio = clamp(event.nativeEvent.locationX / trackWidth, 0, 1);
          onChange(snapToStep(min + ratio * (max - min), min, step));
        },
        onPanResponderMove: (event) => {
          if (trackWidth <= 0) return;

          const ratio = clamp(event.nativeEvent.locationX / trackWidth, 0, 1);
          onChange(snapToStep(min + ratio * (max - min), min, step));
        },
      }),
    [isDisabled, max, min, onChange, step, trackWidth]
  );

  return (
    <View style={[styles.sliderControl, isDisabled && styles.controlDisabled]}>
      <View style={styles.controlHeader}>
        <Text style={styles.controlLabel}>{label}</Text>
        <View style={styles.controlValueRow}>
          <Text style={[styles.controlValue, !isActive && styles.defaultValue]}>
            {valueLabel}
          </Text>
          <Pressable
            disabled={isDisabled || !isActive}
            onPress={onReset}
            style={[
              styles.resetButton,
              (isDisabled || !isActive) && styles.resetButtonDisabled,
            ]}
            testID={resetTestID}
          >
            <Text style={styles.resetButtonText}>Reset</Text>
          </Pressable>
        </View>
      </View>
      <View
        {...panResponder.panHandlers}
        style={styles.sliderHitArea}
        testID={testID}
      >
        <View
          style={styles.sliderTrack}
          onLayout={(event) => setTrackWidth(event.nativeEvent.layout.width)}
        >
          <View style={[styles.sliderFill, { width: `${progress * 100}%` }]} />
          <View style={[styles.sliderThumb, { left: `${progress * 100}%` }]} />
        </View>
      </View>
    </View>
  );
};

export interface ToolbarProps {
  stylesState: StylesState;
  editorRef?: React.RefObject<EnrichedTextInputInstance | null>;
  onOpenLinkModal: () => void;
  onSelectImage: () => void;
  layout?: 'horizontal' | 'grid';
}

export const Toolbar: FC<ToolbarProps> = ({
  stylesState,
  editorRef,
  onOpenLinkModal,
  onSelectImage,
  layout = 'horizontal',
}) => {
  const handlePress = (item: Item) => {
    const currentRef = editorRef?.current;
    if (!currentRef) return;

    switch (item.name) {
      case 'bold':
        editorRef.current?.toggleBold();
        break;
      case 'italic':
        editorRef.current?.toggleItalic();
        break;
      case 'underline':
        editorRef.current?.toggleUnderline();
        break;
      case 'strikethrough':
        editorRef.current?.toggleStrikeThrough();
        break;
      case 'inline-code':
        editorRef?.current?.toggleInlineCode();
        break;
      case 'heading-1':
        editorRef.current?.toggleH1();
        break;
      case 'heading-2':
        editorRef.current?.toggleH2();
        break;
      case 'heading-3':
        editorRef.current?.toggleH3();
        break;
      case 'heading-4':
        editorRef.current?.toggleH4();
        break;
      case 'heading-5':
        editorRef.current?.toggleH5();
        break;
      case 'heading-6':
        editorRef.current?.toggleH6();
        break;
      case 'code-block':
        editorRef?.current?.toggleCodeBlock();
        break;
      case 'quote':
        editorRef?.current?.toggleBlockQuote();
        break;
      case 'unordered-list':
        editorRef.current?.toggleUnorderedList();
        break;
      case 'ordered-list':
        editorRef.current?.toggleOrderedList();
        break;
      case 'checkbox-list':
        // Make checkbox checked by default
        editorRef.current?.toggleCheckboxList(true);
        break;
      case 'link':
        onOpenLinkModal();
        break;
      case 'image':
        onSelectImage();
        break;
      case 'mention':
        editorRef.current?.startMention('@');
        break;
      case 'align-left':
        editorRef.current?.setTextAlignment('left');
        break;
      case 'align-center':
        editorRef.current?.setTextAlignment('center');
        break;
      case 'align-right':
        editorRef.current?.setTextAlignment('right');
        break;
    }
  };

  const isDisabled = (item: Item) => {
    switch (item.name) {
      case 'bold':
        return stylesState.bold.isBlocking;
      case 'italic':
        return stylesState.italic.isBlocking;
      case 'underline':
        return stylesState.underline.isBlocking;
      case 'strikethrough':
        return stylesState.strikeThrough.isBlocking;
      case 'inline-code':
        return stylesState.inlineCode.isBlocking;
      case 'heading-1':
        return stylesState.h1.isBlocking;
      case 'heading-2':
        return stylesState.h2.isBlocking;
      case 'heading-3':
        return stylesState.h3.isBlocking;
      case 'heading-4':
        return stylesState.h4.isBlocking;
      case 'heading-5':
        return stylesState.h5.isBlocking;
      case 'heading-6':
        return stylesState.h6.isBlocking;
      case 'code-block':
        return stylesState.codeBlock.isBlocking;
      case 'quote':
        return stylesState.blockQuote.isBlocking;
      case 'unordered-list':
        return stylesState.unorderedList.isBlocking;
      case 'ordered-list':
        return stylesState.orderedList.isBlocking;
      case 'link':
        return stylesState.link.isBlocking;
      case 'image':
        return stylesState.image.isBlocking;
      case 'mention':
        return stylesState.mention.isBlocking;
      case 'checkbox-list':
        return stylesState.checkboxList.isBlocking;
      default:
        return false;
    }
  };

  const isActive = (item: Item) => {
    switch (item.name) {
      case 'bold':
        return stylesState.bold.isActive;
      case 'italic':
        return stylesState.italic.isActive;
      case 'underline':
        return stylesState.underline.isActive;
      case 'strikethrough':
        return stylesState.strikeThrough.isActive;
      case 'inline-code':
        return stylesState.inlineCode.isActive;
      case 'heading-1':
        return stylesState.h1.isActive;
      case 'heading-2':
        return stylesState.h2.isActive;
      case 'heading-3':
        return stylesState.h3.isActive;
      case 'heading-4':
        return stylesState.h4.isActive;
      case 'heading-5':
        return stylesState.h5.isActive;
      case 'heading-6':
        return stylesState.h6.isActive;
      case 'code-block':
        return stylesState.codeBlock.isActive;
      case 'quote':
        return stylesState.blockQuote.isActive;
      case 'unordered-list':
        return stylesState.unorderedList.isActive;
      case 'ordered-list':
        return stylesState.orderedList.isActive;
      case 'link':
        return stylesState.link.isActive;
      case 'image':
        return stylesState.image.isActive;
      case 'mention':
        return stylesState.mention.isActive;
      case 'checkbox-list':
        return stylesState.checkboxList.isActive;
      case 'align-left':
        return stylesState.alignment === 'left';
      case 'align-center':
        return stylesState.alignment === 'center';
      case 'align-right':
        return stylesState.alignment === 'right';
      default:
        return false;
    }
  };

  const renderItem = ({ item }: ListRenderItemInfo<Item>) => {
    return (
      <ToolbarButton
        {...item}
        testID={`toolbar-${item.name}`}
        isActive={isActive(item)}
        isDisabled={isDisabled(item)}
        onPress={() => handlePress(item)}
        containerStyle={layout === 'grid' ? styles.gridItem : undefined}
      />
    );
  };

  const keyExtractor = (item: Item) => item.name;
  const fontSizeValue = stylesState.fontSize.isActive
    ? stylesState.fontSizeValue
    : FONT_SIZE_FALLBACK;
  const letterSpacingValue = stylesState.letterSpacing.isActive
    ? stylesState.letterSpacingValue
    : LETTER_SPACING_FALLBACK;
  const lineHeightValue = stylesState.lineHeight.isActive
    ? stylesState.lineHeightValue
    : LINE_HEIGHT_FALLBACK;

  return (
    <View style={styles.container}>
      <FlatList
        key={layout}
        numColumns={layout === 'grid' ? GRID_COLUMNS : undefined}
        horizontal={layout === 'horizontal'}
        scrollEnabled={layout === 'horizontal'}
        data={STYLE_ITEMS}
        renderItem={renderItem}
        keyExtractor={keyExtractor}
        style={styles.toolbarList}
        testID="toolbar"
      />
      <View style={styles.textStylePanel} testID="toolbar-text-style-panel">
        <View
          style={[
            styles.fontFamilyControl,
            styles.controlBox,
            stylesState.fontFamily.isBlocking && styles.controlDisabled,
          ]}
        >
          <Text style={styles.controlLabel}>Font family</Text>
          <View style={styles.segmentedControl}>
            {FONT_OPTIONS.map((option) => {
              const isDefault = option.value === null;
              const isSelected = isDefault
                ? !stylesState.fontFamily.isActive
                : stylesState.fontFamily.isActive &&
                  stylesState.fontFamilyValue === option.value;

              return (
                <Pressable
                  key={option.label}
                  disabled={stylesState.fontFamily.isBlocking}
                  onPress={() =>
                    editorRef?.current?.setFontFamily(option.value)
                  }
                  style={[
                    styles.segmentButton,
                    isSelected && styles.segmentButtonActive,
                    stylesState.fontFamily.isBlocking &&
                      styles.segmentButtonDisabled,
                  ]}
                  testID={option.testID}
                >
                  <Text
                    style={[
                      styles.segmentButtonText,
                      isSelected && styles.segmentButtonTextActive,
                    ]}
                  >
                    {option.label}
                  </Text>
                </Pressable>
              );
            })}
          </View>
        </View>
        <TextStyleSlider
          label="Text size"
          value={fontSizeValue}
          valueLabel={
            stylesState.fontSize.isActive
              ? `${formatValue(fontSizeValue, 1)} px`
              : 'Default'
          }
          min={10}
          max={48}
          step={1}
          isActive={stylesState.fontSize.isActive}
          isDisabled={stylesState.fontSize.isBlocking}
          onChange={(value) => editorRef?.current?.setFontSize(value)}
          onReset={() => editorRef?.current?.setFontSize(null)}
          testID="toolbar-font-size-slider"
          resetTestID="toolbar-font-size-reset"
        />
        <TextStyleSlider
          label="Letter spacing"
          value={letterSpacingValue}
          valueLabel={
            stylesState.letterSpacing.isActive
              ? `${formatValue(letterSpacingValue, 0.5)} px`
              : 'Default'
          }
          min={-2}
          max={8}
          step={0.5}
          isActive={stylesState.letterSpacing.isActive}
          isDisabled={stylesState.letterSpacing.isBlocking}
          onChange={(value) => editorRef?.current?.setLetterSpacing(value)}
          onReset={() => editorRef?.current?.setLetterSpacing(null)}
          testID="toolbar-letter-spacing-slider"
          resetTestID="toolbar-letter-spacing-reset"
        />
        <TextStyleSlider
          label="Line height"
          value={lineHeightValue}
          valueLabel={
            stylesState.lineHeight.isActive
              ? `${formatValue(lineHeightValue, 1)} px`
              : 'Default'
          }
          min={14}
          max={72}
          step={1}
          isActive={stylesState.lineHeight.isActive}
          isDisabled={stylesState.lineHeight.isBlocking}
          onChange={(value) => editorRef?.current?.setLineHeight(value)}
          onReset={() => editorRef?.current?.setLineHeight(null)}
          testID="toolbar-line-height-slider"
          resetTestID="toolbar-line-height-reset"
        />
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    width: '100%',
  },
  toolbarList: {
    width: '100%',
  },
  gridItem: {
    flexBasis: `${100 / GRID_COLUMNS}%`,
    aspectRatio: 1,
  },
  textStylePanel: {
    width: '100%',
    padding: 12,
    gap: 12,
    backgroundColor: 'gainsboro',
    borderBottomLeftRadius: 8,
    borderBottomRightRadius: 8,
  },
  controlBox: {
    backgroundColor: 'white',
    borderRadius: 8,
    padding: 12,
  },
  controlDisabled: {
    opacity: 0.45,
  },
  controlHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    gap: 12,
  },
  controlLabel: {
    flexShrink: 1,
    color: 'rgb(0, 26, 114)',
    fontSize: 14,
    fontWeight: '700',
  },
  controlValueRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  controlValue: {
    minWidth: 68,
    color: 'rgb(0, 26, 114)',
    fontSize: 13,
    fontWeight: '600',
    textAlign: 'right',
  },
  defaultValue: {
    color: 'dimgray',
  },
  fontFamilyControl: {
    gap: 10,
  },
  segmentedControl: {
    flexDirection: 'row',
    gap: 8,
  },
  segmentButton: {
    flex: 1,
    minHeight: 36,
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: 'rgb(0, 26, 114)',
    borderRadius: 8,
    paddingHorizontal: 8,
    backgroundColor: 'white',
  },
  segmentButtonActive: {
    backgroundColor: 'rgb(0, 26, 114)',
  },
  segmentButtonDisabled: {
    opacity: 0.8,
  },
  segmentButtonText: {
    color: 'rgb(0, 26, 114)',
    fontSize: 13,
    fontWeight: '700',
    textAlign: 'center',
  },
  segmentButtonTextActive: {
    color: 'white',
  },
  sliderControl: {
    backgroundColor: 'white',
    borderRadius: 8,
    padding: 12,
    gap: 10,
  },
  sliderHitArea: {
    height: 32,
    justifyContent: 'center',
  },
  sliderTrack: {
    height: 6,
    borderRadius: 3,
    backgroundColor: 'lightgray',
  },
  sliderFill: {
    height: 6,
    borderRadius: 3,
    backgroundColor: 'rgb(0, 26, 114)',
  },
  sliderThumb: {
    position: 'absolute',
    top: -7,
    width: 20,
    height: 20,
    marginLeft: -10,
    borderRadius: 10,
    backgroundColor: 'white',
    borderWidth: 2,
    borderColor: 'rgb(0, 26, 114)',
  },
  resetButton: {
    minHeight: 28,
    justifyContent: 'center',
    borderRadius: 8,
    paddingHorizontal: 10,
    backgroundColor: 'rgb(0, 26, 114)',
  },
  resetButtonDisabled: {
    backgroundColor: 'darkgray',
  },
  resetButtonText: {
    color: 'white',
    fontSize: 12,
    fontWeight: '700',
  },
});
