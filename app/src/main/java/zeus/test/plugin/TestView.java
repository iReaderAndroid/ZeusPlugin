package zeus.test.plugin;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import zeus.test.R;

/**
 * Created by huangjian on 2016/12/6.
 * 这是用来测试把宿主中的控件作为公共组件提供给插件
 */
public class TestView extends TextView{
    public TestView(Context context) {
        super(context);
        setBackgroundResource(R.color.test_view_background);
    }

    public TestView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TestView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
}
