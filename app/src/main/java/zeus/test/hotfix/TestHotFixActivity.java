package zeus.test.hotfix;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import zeus.test.R;

/**
 * Created by adison on 16/8/19.
 */
public class TestHotFixActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_testhotfix);
        getSupportActionBar().setTitle("TestHotFix");
    }
}
