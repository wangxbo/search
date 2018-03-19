import org.apache.lucene.analysis.TokenStream;
import org.wltea.analyzer.lucene.IKAnalyzer;

import java.io.IOException;

/**
 * 测试分词器
 * Created by wxb on 2018/3/19.
 */
public class TestIKAnalyzer {

    public static void main(String[] args) throws IOException {

        IKAnalyzer analyzer = new IKAnalyzer();
        TokenStream ts= analyzer.tokenStream("name", "我喜欢你");
        ts.reset();
        while(ts.incrementToken()){
            System.out.println(ts.reflectAsString(false));
        }
    }


    /*startOffset=0,endOffset=1,term=我,bytes=[e6 88 91],type=CN_WORD
    startOffset=1,endOffset=3,term=喜欢,bytes=[e5 96 9c e6 ac a2],type=CN_WORD
    startOffset=3,endOffset=4,term=你,bytes=[e4 bd a0],type=CN_WORD*/


}
