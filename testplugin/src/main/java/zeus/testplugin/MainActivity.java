package zeus.testplugin;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;

import zeus.plugin.ZeusBaseAppCompactActivity;

/**
 * Created by huangjian on 2016/7/8.
 */
public class MainActivity extends ZeusBaseAppCompactActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//       // 动态插件1
//        TextView textView=new TextView(this);
//        textView.setTextColor(getResources().getColor(android.R.color.black));
//        textView.setTextSize(18);
//        textView.setGravity(Gravity.CENTER);
//        textView.setText("这是动态插件");
//        setContentView(textView);
//        getSupportActionBar().setTitle("动态插件");

        //动态插件2
        TextView textView=new TextView(this);
        textView.setTextColor(getResources().getColor(android.R.color.black));
        textView.setTextSize(18);
        textView.setGravity(Gravity.CENTER);
        textView.setText("这是动态插件升级");
        setContentView(textView);
        getSupportActionBar().setTitle("动态插件升级");

    }
}
