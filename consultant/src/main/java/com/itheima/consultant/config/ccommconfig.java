package com.hmdp.config;



import com.hmdp.service.ConsultantService;
import com.hmdp.service.impl.ShopServiceImpl;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.ClassPathDocumentLoader;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class CommonConfig {
    // 流式模型
    @Bean
    public OpenAiStreamingChatModel openAiStreamingChatModel() {
        return OpenAiStreamingChatModel.builder()
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1/")//qwen模型
                .apiKey(System.getenv("sk-857fa40466c04357a85cf54ae4757cc7"))//密钥，用你自己的，我这放环境变量了
                .modelName("qwen-plus")
                .logRequests(true) //输出日志
                .logResponses(true)//输出日志
                .build();
    }


    // 流式 AI 服务
    @Bean
    public ConsultantService consultantStreamingService(
            OpenAiStreamingChatModel streamingModel,
            ChatMemoryProvider chatMemoryProvider,
            ContentRetriever contentRetriever) {

        return AiServices.builder(ConsultantService.class)
                .streamingChatModel(streamingModel)
                .chatMemoryProvider(chatMemoryProvider)
                .contentRetriever(contentRetriever)
                .systemMessageProvider(chatMemoryId -> """
                    你是本地吃喝玩乐推荐助手，请遵守以下规则：
                    
                    1. 推荐格式要求：
                       - 使用自然流畅的中文
                       - 每个推荐店铺独占一行
                       - 格式：店名 - 人均价格 - 评分 - 简短评价
                       - 示例：老灶火锅 - 人均80元 - 4.5分 - 麻辣鲜香，环境舒适
                    
                    2. 回答规则：
                       - 只能基于检索到的店铺数据回答
                       - 如果信息缺失，用"暂无"代替
                       - 不要输出任何模板代码或占位符
                       - 不要输出 {{}} 这样的模板语法
                    
                    3. 回答示例：
                       为您推荐以下店铺：
                       
                       川味观 - 人均90元 - 4.7分 - 正宗川菜，毛肚很新鲜
                       海底捞 - 人均120元 - 4.8分 - 服务好，24小时营业
                       绿茶餐厅 - 人均60元 - 4.3分 - 性价比高，适合聚餐
                       
                       以上是根据您的需求推荐的店铺，希望对您有帮助！
                    """)
                .build();
    }

    //构建ChatMemoryProvider对象 ，用来记录历史记录的
    @Bean
    public ChatMemoryProvider chatMemoryProvider() {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(20)
                .build();
    }
    // 1. 配置嵌入模型（用于将文本转为向量）
    @Bean
    public OpenAiEmbeddingModel embeddingModel() {
        return OpenAiEmbeddingModel.builder()
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1/")
                .apiKey("sk-857fa40466c04357a85cf54ae4757cc7")  // 使用相同的密钥
                .modelName("text-embedding-v1")
                .build();
    }

    // 2. 配置向量存储（这里用内存存储，生产可替换为Pinecone/Milvus等）
    @Bean
    public EmbeddingStore embeddingStore(EmbeddingModel embeddingModel, ShopServiceImpl shopService) {
        // 从数据库加载店铺数据转换为文档
        List<Document> documents = shopService.list().stream()
                .map(shop -> Document.from(
                        "店铺名称：" + shop.getName() + "\n" +
                                "地址：" + shop.getAddress() + "\n" +
                                "id：" + shop.getId() + "\n" +
                                "人均：" + shop.getAvgPrice() + "\n" +
                                "评分：" + (shop.getScore() != null ? shop.getScore() : "")
                ))
                .collect(Collectors.toList());

        // 过滤空文档
        documents.removeIf(doc -> doc.text().trim().isEmpty());

        // 文档分块（避免超出模型token限制）
        List<Document> chunkedDocs = new ArrayList<>();
        for (Document doc : documents) {
            String text = doc.text();
            int chunkSize = 500; // 按500字符分块
            for (int i = 0; i < text.length(); i += chunkSize) {
                int end = Math.min(text.length(), i + chunkSize);
                chunkedDocs.add(Document.from(text.substring(i, end)));
            }
        }

        // 初始化向量存储并写入文档
        InMemoryEmbeddingStore store = new InMemoryEmbeddingStore();
        EmbeddingStoreIngestor.builder()
                .embeddingStore(store)
                .embeddingModel(embeddingModel)
                .build()
                .ingest(chunkedDocs);

        return store;
    }
    // 3. 配置内容检索器（从向量库检索相关文档）
    @Bean
    public ContentRetriever contentRetriever(EmbeddingStore embeddingStore, EmbeddingModel embeddingModel) {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .minScore(0.6) // 最低匹配分数（0-1，越高越精准）
                .maxResults(5) // 最多返回5条相关文档
                .build();
    }


}
