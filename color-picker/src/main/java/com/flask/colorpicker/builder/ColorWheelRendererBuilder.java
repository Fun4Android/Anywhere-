package com.flask.colorpicker.builder;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.renderer.ColorWheelRenderer;
import com.flask.colorpicker.renderer.FlowerColorWheelRenderer;
import com.flask.colorpicker.renderer.SimpleColorWheelRenderer;

public class ColorWheelRendererBuilder {
	public static ColorWheelRenderer getRenderer(ColorPickerView.WHEEL_TYPE wheelType) {
    return switch (wheelType) {
      case CIRCLE -> new SimpleColorWheelRenderer();
      case FLOWER -> new FlowerColorWheelRenderer();
    };
  }
}
