package com.investment.portal.mapper;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.Resource;

import javax.sql.DataSource;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * DB 연결 없이도 MyBatis 가 매퍼 XML을 파싱하고 인터페이스에 바인딩할 수 있는지만
 * 확인한다. XML `<select id="...">` 오타나 resultType FQN 오류는 컴파일 시점에는
 * 안 걸리고 Spring 컨텍스트 기동 시점에야 BindingException 으로 드러난다 —
 * 이 환경(Docker 없음)은 실제 MySQL 로 전체 컨텍스트를 띄울 수 없어, 그 구간만
 * 따로 잘라서 검증한다.
 *
 * <p>application.yml 의 mapper-locations(classpath*:mapper/**{@literal /}*.xml)와
 * 동일한 패턴으로 XML을 전부 로드한다.
 */
class MapperXmlBindingTest {

    @Test
    void 전체_매퍼_XML이_예외_없이_파싱된다() throws Exception {
        DataSource dummyDataSource = mock(DataSource.class);

        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dummyDataSource);
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] mapperLocations = resolver.getResources("classpath*:mapper/**/*.xml");
        assertThat(mapperLocations).isNotEmpty();
        factoryBean.setMapperLocations(mapperLocations);
        factoryBean.setTypeAliasesPackage("com.investment");

        SqlSessionFactory factory = factoryBean.getObject();
        assertThat(factory).isNotNull();
    }

    @Test
    void 이번에_추가한_쿼리_id가_실제로_등록돼_있다() throws Exception {
        DataSource dummyDataSource = mock(DataSource.class);
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dummyDataSource);
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        factoryBean.setMapperLocations(resolver.getResources("classpath*:mapper/**/*.xml"));
        factoryBean.setTypeAliasesPackage("com.investment");

        Configuration config = factoryBean.getObject().getConfiguration();

        Set<String> statementIds = config.getMappedStatementNames().stream()
                .filter(n -> !n.contains("!selectKey"))
                .collect(Collectors.toSet());

        assertThat(statementIds)
                .contains(
                        "com.investment.portal.domain.repository.stock.StockPriceHistoryMapper.findMaxChangeDay",
                        "com.investment.portal.domain.repository.stock.StockPriceHistoryMapper.findClosestOnOrAfter",
                        "com.investment.portal.domain.repository.stock.StockPriceHistoryMapper.findClosestOnOrBefore",
                        "com.investment.portal.domain.repository.stock.StockPriceHistoryMapper.findPeriodHighDay",
                        "com.investment.portal.domain.repository.stock.StockPriceHistoryMapper.findPeriodLowDay",
                        "com.investment.analyzer.market_analyzer.domain.repository.news.NewsMapper.findByDateAndKeyword");
    }
}
