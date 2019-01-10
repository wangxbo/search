
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.wltea.analyzer.lucene.IKAnalyzer;

/**
 * Created by wxb on 2018/3/16.
 */
public class TestLucene {

    public static void main(String[] args) throws Exception {
        // 1. 准备中文分词器
        IKAnalyzer analyzer = new IKAnalyzer();

        // 2. 索引
        List<String> productNames = new ArrayList();
        productNames.add("飞利浦led灯泡e27螺口暖白球泡灯家用照明超亮节能灯泡转色温灯泡");
        productNames.add("飞利浦led灯泡e14螺口蜡烛灯泡3W尖泡拉尾节能灯泡暖黄光源Lamp");
        productNames.add("雷士照明 LED灯泡 e27大螺口节能灯3W球泡灯 Lamp led节能灯泡");
        productNames.add("飞利浦 led灯泡 e27螺口家用3w暖白球泡灯节能灯5W灯泡LED单灯7w");
        productNames.add("飞利浦led小球泡e14螺口4.5w透明款led节能灯泡照明光源lamp单灯");
        productNames.add("飞利浦蒲公英护眼台灯工作学习阅读节能灯具30508带光源");
        productNames.add("欧普照明led灯泡蜡烛节能灯泡e14螺口球泡灯超亮照明单灯光源");
        productNames.add("欧普照明led灯泡节能灯泡超亮光源e14e27螺旋螺口小球泡暖黄家用");
        productNames.add("聚欧普照明led灯泡节能灯泡e27螺口球泡家用led照明单灯超亮光源");
        //1. 首先准备10条数据 这10条数据都是字符串，相当于产品表里的数据
        //2. 通过createIndex方法，把它加入到索引当中
        Directory index = createIndex(analyzer, productNames);

        // 3. 查询器
        //根据关键字 护眼带光源，基于 "name" 字段进行查询。
        //这个 "name" 字段就是在创建索引步骤里每个Document的 "name" 字段，相当于表的字段名
        String keyword = "护眼带光源";
        Query query = new QueryParser(Version.LUCENE_47, "name", analyzer).parse(keyword);


        // 4. 搜索
        //创建索引 reader:
        IndexReader reader = DirectoryReader.open(index);
        //基于 reader 创建搜索器
        IndexSearcher searcher = new IndexSearcher(reader);
        //指定每页要显示多少条数据
        int numberPerPage = 1000;
        System.out.printf("当前一共有%d条数据%n",productNames.size());
        System.out.printf("查询关键字是：\"%s\"%n",keyword);
        //执行搜索,每一个ScoreDoc[] hits 就是一个搜索结果，首先把他遍历出来
        ScoreDoc[] hits = searcher.search(query, numberPerPage).scoreDocs;

        // 5. 显示查询结果
        showSearchResults(searcher, hits, query, analyzer);
        // 6. 关闭查询
        reader.close();
    }

    private static void showSearchResults(IndexSearcher searcher, ScoreDoc[] hits, Query query, IKAnalyzer analyzer)
            throws Exception {
        System.out.println("找到 " + hits.length + " 个命中.");
        System.out.println("序号\t匹配度得分\t结果");
        for (int i = 0; i < hits.length; ++i) {
            ScoreDoc scoreDoc= hits[i];
            //然后获取当前结果的docid, 这个docid相当于就是这个数据在索引中的主键
            int docId = scoreDoc.doc;
            //再根据主键docid，通过搜索器从索引里把对应的Document取出来
            Document d = searcher.doc(docId);
            List<IndexableField> fields = d.getFields();
            //接着就打印出这个Document里面的数据。 虽然当前Document只有name一个字段，但是代码还是通过遍历所有字段的形式，打印出里面的值，
            // 这样当Docment有多个字段的时候，代码就不用修改了，兼容性更好点。
            //scoreDoc.score 表示当前命中的匹配度得分，越高表示匹配程度越高
            System.out.print((i + 1));
            System.out.print("\t" + scoreDoc.score);
            for (IndexableField f : fields) {
                System.out.print("\t" + d.get(f.name()));
            }
            System.out.println();
        }
    }

    private static Directory createIndex(IKAnalyzer analyzer, List<String> products) throws IOException {
        //创建内存索引，为什么Lucene会比数据库快？因为它是从内存里查，自然就比数据库里快多了呀
        Directory index = new RAMDirectory();
        //根据中文分词器创建配置对象
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_47, analyzer);
        //创建索引 writer
        IndexWriter writer = new IndexWriter(index, config);
        //遍历那10条数据，把他们挨个放进索引里
        for (String name : products) {
            addDoc(writer, name);
        }
        writer.close();
        return index;
    }


    //每条数据创建一个Document，并把这个Document放进索引里。 这个Document有一个字段，叫做"name"。 TestLucene.java 第49行创建查询器，就会指定查询这个字段
    //Query query = new QueryParser(Version.LUCENE_47, "name", analyzer).parse(keyword);
    private static void addDoc(IndexWriter w, String name) throws IOException {
        Document doc = new Document();
        doc.add(new TextField("name", name, Field.Store.YES));
        w.addDocument(doc);
    }
}


        /*和Like的区别
        like 也可以进行查询，那么使用lucene 的方式有什么区别呢？ 主要是两点：
        1. 相关度
        通过观察运行结果，可以看到不同相关度的结果都会查询出来，但是使用 like，就做不到这一点了
        2. 性能
        数据量小的时候，like 也会有很好的表现，但是数据量一大，like 的表现就差很多了。 在接下来的教程里会演示对 14万条数据 的查询*/


       /* 思路：
                现在通过自己做了一遍 Lucene了，有了感性的认识，接着来整理一下做 Lucene的思路。
                1. 首先搜集数据
                数据可以使文件系统，数据库，网络上，手工输入的，或者像本例直接写在内存上的
                2. 通过数据创建索引
                3. 用户输入关键字
                4. 通过关键字创建查询器
                5. 根据查询器到索引里获取数据
                6. 然后把查询结果展示在用户面前*/
