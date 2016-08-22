package zeus.test.hotfix;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.widget.TextView;

import zeus.plugin.ZeusBaseAppCompactActivity;

/**
 * 补丁测试页面
 *
 * @author adison
 * @date 16/8/21
 * @time 上午2:04
 */
public class TestHotFixActivity extends ZeusBaseAppCompactActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView textView = new TextView(this);
        textView.setTextColor(getResources().getColor(android.R.color.black));
        textView.setTextSize(18);
        textView.setGravity(Gravity.CENTER);
        textView.setText(new TestHotFix().getTestString());
        setContentView(textView);
        getSupportActionBar().setTitle(new TestHotFix().getTestString2());
    }

    public static String getString(){
        return "页面";
    }
}
