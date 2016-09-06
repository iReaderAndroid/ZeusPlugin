package zeus.test.hotfix;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;

import zeus.plugin.ZeusBaseAppCompactActivity;
import zeus.test.R;

/**
 * 补丁测试页面
 *
 * @author adison
 * @date 16/8/21
 * @time 上午2:04
 */
public class TestHotFixActivity extends ZeusBaseAppCompactActivity {

    @Override
    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView textView = (TextView) findViewById(R.id.text_view);
        //如果是红色说明走的是补丁类
        //activity_main中textSize也变大了，是25dp
        textView.setTextColor(getResources().getColor(android.R.color.holo_red_light));
        textView.setGravity(Gravity.CENTER);
        textView.setText(new TestHotFix().getTestString());
        getSupportActionBar().setTitle(new TestHotFix().getTestString2());
    }

    public static String getString(){
        return "页面";
    }
}
