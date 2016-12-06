package zeus.test.hotfix;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;

import zeus.plugin.ZeusBaseActivity;
import zeus.test.R;

/**
 * 补丁测试页面
 *
 * @author adison
 * @date 16/8/21
 * @time 上午2:04
 */
public class TestHotFixActivity extends ZeusBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_testhotfix);
        TextView textView = (TextView) findViewById(R.id.text_view);
        textView.setTextColor(getResources().getColor(android.R.color.black));
        textView.setTextSize(18);
        textView.setGravity(Gravity.CENTER);
        textView.setText(new TestHotFix().getTestString());
        setContentView(textView);
        setTitle(new TestHotFix().getTestString2());
    }

    public static String getString(){
        return "页面";
    }
}
