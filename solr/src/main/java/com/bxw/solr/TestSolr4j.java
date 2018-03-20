package com.bxw.solr;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;


public class TestSolr4j {

	public static void main(String[] args) throws Exception {
		//添加记录
		//batchSave();

		//分页查询
		//query("name:手机");

		//高亮显示,这里前面必须写上关键字所对应的字段名,
		//SolrUtil.queryHighlight("place:山东济南");

		//修改、删除索引
		//updateOrDelete();

	}


	/**
	 * 测试添加索引
	 * @throws SolrServerException
	 * @throws IOException
	 */
	public static void batchSave() throws SolrServerException, IOException{
		List<Product> products = ProductUtil.file2list("140k_products.txt");
		SolrUtil.batchSaveOrUpdate(products);
	}


	/**
	 * 测试分页查询
	 */
	public static void query(String keywords) throws Exception{
		//查询
		QueryResponse queryResponse = SolrUtil.query(keywords,0,10);
		SolrDocumentList documents=	queryResponse.getResults();
		System.out.println("累计找到的条数："+documents.getNumFound());
		if(!documents.isEmpty()){
			Collection<String> fieldNames = documents.get(0).getFieldNames();
			for (String fieldName : fieldNames) {
				System.out.print(fieldName+"\t");
			}
			System.out.println();
		}

		for (SolrDocument solrDocument : documents) {
			Collection<String> fieldNames= solrDocument.getFieldNames();
			for (String fieldName : fieldNames) {
				System.out.print(solrDocument.get(fieldName)+"\t");
			}
			System.out.println();

		}
	}


	public static void updateOrDelete() throws Exception{
		String keyword = "name:鞭";
		System.out.println("修改之前");
		query(keyword);

		Product p = new Product();
		p.setId(51173);
		p.setName("修改后的神鞭");
		SolrUtil.saveOrUpdate(p);
		System.out.println("修改之后");
		query(keyword);

		SolrUtil.deleteById("51173");
		System.out.println("删除之后");
		query(keyword);
	}

}
