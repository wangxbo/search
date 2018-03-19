package test140k;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.wltea.analyzer.lucene.IKAnalyzer;


/**
 * 测试14万条数据
 * 为了模仿真实环境，花了很多精力，四处搜刮来了14万条天猫的产品数据，接下来我们就会把这14万条记录加入到 Lucene,然后观察搜索效果。
 * 1. 索引的增加，以前是10条数据，现在是14万条数据
 注： 因为数据量比较大， 所以加入到索引的时间也比较久，请耐心等待。
 2. Document以前只有name字段，现在有6个字段
 3. 查询关键字从控制台输入，这样每次都可以输入不同的关键字进行查询。 因为索引建立时间比较久，采用这种方式，可以建立一次索引，进行多次查询，否则的话，每次使用不同的关键字，都要耗时建立索引，测试效率会比较低
 * Created by wxb on 2018/3/19.
 */
public class TestLucene {

	public static void main(String[] args) throws Exception {
		// 1. 准备中文分词器
		IKAnalyzer analyzer = new IKAnalyzer();
		// 2. 索引
		Directory index = createIndex(analyzer);
		// 3. 查询器
        Scanner s = new Scanner(System.in);

		/*****************索引的删除和更新***************/
		//索引删除，删除id=51173的数据，通过关键字 “鞭" 可以查询到一条id是51173的数据
		//删除后就搜不到了
		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_47, analyzer);
		IndexWriter indexWriter = new IndexWriter(index, config);
		indexWriter.deleteDocuments(new Term("id", "51173"));
		indexWriter.commit();
		indexWriter.close();

		/*还可以按照如下方法来删除索引，API 很明显，就不做代码示例了

		DeleteDocuments(Query query):根据Query条件来删除单个或多个Document
		DeleteDocuments(Query[] queries):根据Query条件来删除单个或多个Document
		DeleteDocuments(Term term):根据Term来删除单个或多个Document
		DeleteDocuments(Term[] terms):根据Term来删除单个或多个Document
		DeleteAll():删除所有的Document*/

		// 更新索引
		/*IndexWriterConfig config = new IndexWriterConfig(analyzer);
		IndexWriter indexWriter = new IndexWriter(index, config);
		Document doc = new Document();
		doc.add(new TextField("id", "51173", Field.Store.YES));
		doc.add(new TextField("name", "神鞭，鞭没了，神还在", Field.Store.YES));
		doc.add(new TextField("category", "道具", Field.Store.YES));
		doc.add(new TextField("price", "998", Field.Store.YES));
		doc.add(new TextField("place", "南海群岛", Field.Store.YES));
		doc.add(new TextField("code", "888888", Field.Store.YES));
		indexWriter.updateDocument(new Term("id", "51173"), doc );
		indexWriter.commit();
		indexWriter.close();*/

        
        while(true){
        	System.out.print("请输入查询关键字：");
            String keyword = s.nextLine();
            System.out.println("当前关键字是："+keyword);
    		Query query = new QueryParser(Version.LUCENE_47,  "name", analyzer).parse(keyword);

    		// 4. 搜索
    		IndexReader reader = DirectoryReader.open(index);
    		IndexSearcher searcher=new IndexSearcher(reader);
    		int numberPerPage = 10;
    		//ScoreDoc[] hits = searcher.search(query, numberPerPage).scoreDocs;

			//使用分页
			int pageNow = 1;
			int pageSize = 10;
			ScoreDoc[] hits = pageSearch2(query, searcher, pageNow, pageSize);

    		// 5. 显示查询结果
    		showSearchResults(searcher, hits,query,analyzer);
    		// 6. 关闭查询
    		reader.close();
        }
	}


	/*两种分页比较：
	分页查询是很常见的需求，比如要查询第10页，每页10条数据。
	Lucene 分页通常来讲有两种方式：
	第一种是把100条数据查出来，然后取最后10条。 优点是快，缺点是对内存消耗大。
	第二中是把第90条查询出来，然后基于这一条，通过searchAfter方法查询10条数据。 优点是内存消耗小，缺点是比第一种更慢*/
	/**
	 * 第一种分页查询
	 * @param query
	 * @param searcher
	 * @param pageNow
	 * @param pageSize
	 * @return
	 * @throws IOException
	 */
	private static ScoreDoc[] pageSearch1(Query query, IndexSearcher searcher, int pageNow, int pageSize)
			throws IOException {
		TopDocs topDocs = searcher.search(query, pageNow*pageSize);
		System.out.println("查询到的总条数\t"+topDocs.totalHits);
		ScoreDoc [] alllScores = topDocs.scoreDocs;

		List<ScoreDoc> hitScores = new ArrayList<>();

		int start = (pageNow -1)*pageSize ;
		int end = pageSize*pageNow;

		if(alllScores.length >= end ){
			for(int i=start;i<end;i++)
				hitScores.add(alllScores[i]);
		}else if(alllScores.length >= start){
			for(int i=start;i<alllScores.length;i++)
				hitScores.add(alllScores[i]);
		}

		ScoreDoc[] hits = hitScores.toArray(new ScoreDoc[]{});
		return hits;
	}


	/**
	 * 第二种分页查询
	 * @param query
	 * @param searcher
	 * @param pageNow
	 * @param pageSize
	 * @return
	 * @throws IOException
	 */
	private static ScoreDoc[] pageSearch2(Query query, IndexSearcher searcher, int pageNow, int pageSize)
			throws IOException {

		int start = (pageNow - 1) * pageSize;
		if(0==start){
			TopDocs topDocs = searcher.search(query, pageNow*pageSize);
			return topDocs.scoreDocs;
		}
		// 查询数据， 结束页面自前的数据都会查询到，但是只取本页的数据
		TopDocs topDocs = searcher.search(query, start);
		//获取到上一页最后一条

		ScoreDoc preScore= topDocs.scoreDocs[start-1];

		//查询最后一条后的数据的一页数据
		topDocs = searcher.searchAfter(preScore, query, pageSize);
		return topDocs.scoreDocs;

	}




	private static void showSearchResults(IndexSearcher searcher, ScoreDoc[] hits, Query query, IKAnalyzer analyzer) throws Exception {
		System.out.println("找到 " + hits.length + " 个命中.");

        SimpleHTMLFormatter simpleHTMLFormatter = new SimpleHTMLFormatter("<span style='color:red'>", "</span>");
        Highlighter highlighter = new Highlighter(simpleHTMLFormatter, new QueryScorer(query));

        System.out.println("找到 " + hits.length + " 个命中.");
        System.out.println("序号\t匹配度得分\t结果");
		for (int i = 0; i < hits.length; ++i) {
			ScoreDoc scoreDoc= hits[i];
			int docId = scoreDoc.doc;
			Document d = searcher.doc(docId);
			List<IndexableField> fields= d.getFields();
			System.out.print((i + 1) );
			System.out.print("\t" + scoreDoc.score);
			for (IndexableField f : fields) {
				if("name".equals(f.name())){
		            TokenStream tokenStream = analyzer.tokenStream(f.name(), new StringReader(d.get(f.name())));
		            String fieldContent = highlighter.getBestFragment(tokenStream, d.get(f.name()));
					System.out.print("\t"+fieldContent);
				}
				else{
					System.out.print("\t"+d.get(f.name()));
				}
			}
			System.out.println("<br>");
		}
	}

	private static Directory createIndex(IKAnalyzer analyzer) throws IOException {
		Directory index = new RAMDirectory();
		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_47, analyzer);
		IndexWriter writer = new IndexWriter(index, config);
		String fileName = "140k_products.txt";
		List<Product> products = ProductUtil.file2list(fileName);
		int total = products.size();
		int count = 0;
		int per = 0;
		int oldPer =0;
		for (Product p : products) {
			addDoc(writer, p);
			count++;
			per = count*100/total;
			if(per!=oldPer){
				oldPer = per;
				System.out.printf("索引中，总共要添加 %d 条记录，当前添加进度是： %d%% %n",total,per);
			}
			
		}
		writer.close();
		return index;
	}

	private static void addDoc(IndexWriter w, Product p) throws IOException {
		Document doc = new Document();
		doc.add(new TextField("id", String.valueOf(p.getId()), Field.Store.YES));
		doc.add(new TextField("name", p.getName(), Field.Store.YES));
		doc.add(new TextField("category", p.getCategory(), Field.Store.YES));
		doc.add(new TextField("price", String.valueOf(p.getPrice()), Field.Store.YES));
		doc.add(new TextField("place", p.getPlace(), Field.Store.YES));
		doc.add(new TextField("code", p.getCode(), Field.Store.YES));
		w.addDocument(doc);
	}
}
