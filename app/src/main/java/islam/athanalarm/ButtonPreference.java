package islam.athanalarm;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;

public class ButtonPreference extends Preference {

    public ButtonPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ButtonPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        Button button = view.findViewById(R.id.button);
        if (button != null) {
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getOnPreferenceClickListener() != null) {
                        getOnPreferenceClickListener().onPreferenceClick(ButtonPreference.this);
                    }
                }
            });
        }
    }
}
