package org.nuclearfog.twidda.window;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Spinner;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.OnColorChangedListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;

import org.nuclearfog.twidda.R;
import org.nuclearfog.twidda.adapter.WorldIdAdapter;
import org.nuclearfog.twidda.backend.TwitterEngine;
import org.nuclearfog.twidda.database.GlobalSettings;

/**
 * App settings page
 *
 * @see GlobalSettings
 */
public class AppSettings extends AppCompatActivity implements OnClickListener,
        OnDismissListener, OnItemSelectedListener, OnCheckedChangeListener {

    private static final int BACKGROUND = 0;
    private static final int FONTCOLOR = 1;
    private static final int HIGHLIGHT = 2;
    private static final int POPUPCOLOR = 3;

    private GlobalSettings settings;
    private Button colorButton1, colorButton2, colorButton3, colorButton4;
    private CheckBox toggleImg, toggleAns;
    private EditText woeIdText;
    private Spinner woeId;
    private View root;

    private int color = 0;
    private int mode = 0;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.page_settings);

        Toolbar toolbar = findViewById(R.id.toolbar_setting);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle(R.string.settings);

        settings = GlobalSettings.getInstance(this);

        Button delButton = findViewById(R.id.delete_db);
        Button load_popup = findViewById(R.id.load_dialog);
        Button logout = findViewById(R.id.logout);
        colorButton1 = findViewById(R.id.color_background);
        colorButton2 = findViewById(R.id.color_font);
        colorButton3 = findViewById(R.id.color_popup);
        colorButton4 = findViewById(R.id.highlight_color);
        toggleImg = findViewById(R.id.toggleImg);
        toggleAns = findViewById(R.id.toggleAns);
        woeIdText = findViewById(R.id.woe_id);
        woeId = findViewById(R.id.woeid);
        root = findViewById(R.id.settings_layout);
        root.setBackgroundColor(settings.getBackgroundColor());

        load_popup.setOnClickListener(this);
        delButton.setOnClickListener(this);
        logout.setOnClickListener(this);
        colorButton1.setOnClickListener(this);
        colorButton2.setOnClickListener(this);
        colorButton3.setOnClickListener(this);
        colorButton4.setOnClickListener(this);
        woeId.setOnItemSelectedListener(this);
    }


    @Override
    protected void onStart() {
        super.onStart();
        toggleImg.setChecked(settings.getImageLoad());
        toggleAns.setChecked(settings.getAnswerLoad());
        woeId.setAdapter(new WorldIdAdapter(this));
        woeId.setSelection(settings.getWoeIdSelection());
        colorButton1.setBackgroundColor(settings.getBackgroundColor());
        colorButton2.setBackgroundColor(settings.getFontColor());
        colorButton3.setBackgroundColor(settings.getPopupColor());
        colorButton4.setBackgroundColor(settings.getHighlightColor());
        if (settings.getCustomWidSet()) {
            String text = Long.toString(settings.getWoeId());
            woeIdText.setVisibility(View.VISIBLE);
            woeIdText.setText(text);
        }
        toggleImg.setOnCheckedChangeListener(this);
        toggleAns.setOnCheckedChangeListener(this);
    }


    @Override
    public void onBackPressed() {
        if (settings.getCustomWidSet()) {
            String woeText = woeIdText.getText().toString();
            if (!woeText.isEmpty())
                settings.setWoeId(Long.parseLong(woeText));
            else
                settings.setWoeId(1);
        }
        super.onBackPressed();
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.delete_db:
                new Builder(this)
                        .setMessage(R.string.delete_database_popup)
                        .setNegativeButton(R.string.no_confirm, null)
                        .setPositiveButton(R.string.yes_confirm, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                deleteDatabase("database.db");
                            }
                        })
                        .show();
                break;

            case R.id.logout:
                new Builder(this)
                        .setMessage(R.string.confirm_log_lout)
                        .setNegativeButton(R.string.no_confirm, null)
                        .setPositiveButton(R.string.yes_confirm, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                settings.logout();
                                TwitterEngine.destroyInstance();
                                deleteDatabase("database.db");
                                finish();
                            }
                        })
                        .show();
                break;

            case R.id.color_background:
                mode = BACKGROUND;
                color = settings.getBackgroundColor();
                setColor(color);
                break;

            case R.id.color_font:
                mode = FONTCOLOR;
                color = settings.getFontColor();
                setColor(color);
                break;

            case R.id.color_popup:
                mode = POPUPCOLOR;
                color = settings.getPopupColor();
                setColor(color);
                break;

            case R.id.highlight_color:
                mode = HIGHLIGHT;
                color = settings.getHighlightColor();
                setColor(color);
                break;

            case R.id.load_dialog:
                Dialog load_popup = new Dialog(this);
                final NumberPicker load = new NumberPicker(this);
                load.setMaxValue(100);
                load.setMinValue(10);
                load.setValue(settings.getRowLimit());
                load.setWrapSelectorWheel(false);
                load_popup.setContentView(load);
                load_popup.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if (settings.getRowLimit() != load.getValue()) {
                            settings.setRowLimit(load.getValue());
                        }
                    }
                });
                load_popup.show();
                break;
        }
    }


    @Override
    public void onDismiss(DialogInterface d) {
        switch (mode) {
            case BACKGROUND:
                root.setBackgroundColor(color);
                settings.setBackgroundColor(color);
                colorButton1.setBackgroundColor(color);
                break;

            case FONTCOLOR:
                settings.setFontColor(color);
                colorButton2.setBackgroundColor(color);
                break;

            case POPUPCOLOR:
                settings.setPopupColor(color);
                colorButton3.setBackgroundColor(color);
                break;

            case HIGHLIGHT:
                settings.setHighlightColor(color);
                colorButton4.setBackgroundColor(color);
                break;
        }
    }


    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (position == parent.getCount() - 1) {
            woeIdText.setVisibility(View.VISIBLE);
            settings.setCustomWidSet(true);
            settings.setWoeId(1);
        } else {
            woeIdText.setVisibility(View.INVISIBLE);
            woeIdText.setText("");
            settings.setCustomWidSet(false);
            settings.setWoeId(id);
        }
        settings.setWoeIdSelection(position);
    }


    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        woeId.setSelection(settings.getWoeIdSelection());
    }


    @Override
    public void onCheckedChanged(CompoundButton c, boolean checked) {
        switch (c.getId()) {
            case R.id.toggleImg:
                settings.setImageLoad(checked);
                break;

            case R.id.toggleAns:
                settings.setAnswerLoad(checked);
                break;
        }
    }


    private void setColor(int preColor) {
        Dialog d = ColorPickerDialogBuilder.with(this)
                .showAlphaSlider(false).initialColor(preColor)
                .wheelType(ColorPickerView.WHEEL_TYPE.CIRCLE).density(20)
                .setOnColorChangedListener(new OnColorChangedListener() {
                    @Override
                    public void onColorChanged(int i) {
                        color = i;
                    }
                }).build();
        d.setOnDismissListener(this);
        d.show();
    }
}