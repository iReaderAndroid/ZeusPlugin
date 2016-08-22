package zeus.test.hotfix;

/**
 * Created by huangjian on 2016/8/22.
 */
public class TestHotFix {
    public String getTestString(){
        return "这是补丁";
    }

    public String getTestString2(){
        return getTestString() + TestHotFixActivity.getString();
    }
}
