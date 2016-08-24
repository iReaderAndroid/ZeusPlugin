package zeus.test.hotfix;

/**
 * Created by huangjian on 2016/8/22.
 */
public class TestHotFix {
    String test = "补丁";
    public String getTestString(){
        return "这是" + getTestString3();
    }

    public String getTestString2(){
        return getTestString() + TestHotFixActivity.getString();
    }

    public String getTestString3(){
        return test;
    }
}
