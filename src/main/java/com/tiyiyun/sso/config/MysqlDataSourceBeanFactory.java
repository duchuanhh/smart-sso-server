package com.tiyiyun.sso.config;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import com.alibaba.druid.pool.DruidDataSource;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class MysqlDataSourceBeanFactory {
	@Bean("mysqlDataSourceConfig")
	@ConfigurationProperties(prefix = "spring.datasource.mysql")
	public DataSourceConfig createDataSourceConfig() {
		return new DataSourceConfig();
	}
	
	
	@Bean(name = "mysqlDataSource")
    @Primary
    public DataSource dataSource(@Qualifier("mysqlDataSourceConfig") DataSourceConfig config
    	) {
		DruidDataSource datasource = new DruidDataSource();
        datasource.setUrl(config.getUrl());     
        String UserInfoEntityName = config.getUserInfoEntityname();
        String passWord = config.getPassword();
        datasource.setUsername(UserInfoEntityName);
    	datasource.setPassword(passWord);
        datasource.setDriverClassName(config.getDriverClassName());
        datasource.setInitialSize(config.getInitialSize());
        datasource.setMinIdle(config.getMinIdle());
        datasource.setMaxActive(config.getMaxActive());
        datasource.setMaxWait(config.getMaxWait());
        datasource.setTimeBetweenEvictionRunsMillis(config.getTimeBetweenEvictionRunsMillis());
        datasource.setMinEvictableIdleTimeMillis(config.getMinEvictableIdleTimeMillis());
        datasource.setValidationQuery(config.getValidationQuery());
        datasource.setTestWhileIdle(config.isTestWhileIdle());
        datasource.setTestOnBorrow(config.isTestOnBorrow());
        datasource.setTestOnReturn(config.isTestOnReturn());
        datasource.setPoolPreparedStatements(config.isPoolPreparedStatements());
        datasource.setMaxOpenPreparedStatements(config.getMaxOpenPreparedStatements());
        try {
            datasource.setFilters(config.getFilters());
        } catch (SQLException e) {
        	log.error("druid configuration initialization filter {}", e.getMessage());
        }
        return datasource;
    }

	@Bean(name = "mysqlSqlSessionFactory")
    @Primary
    public SqlSessionFactory sqlSessionFactory(@Qualifier("mysqlDataSource") DataSource dataSource) throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(dataSource);
        bean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath:/com/tiyiyun/mapping/*.xml"));
        return bean.getObject();
    }
	
	@Bean(name = "mysqlTransactionManager")
    @Primary
    public DataSourceTransactionManager transactionManager(@Qualifier("mysqlDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
 
    @Bean(name = "mysqlSqlSessionTemplate")
    @Primary
    public SqlSessionTemplate sqlSessionTemplate(@Qualifier("mysqlSqlSessionFactory") SqlSessionFactory sqlSessionFactory)
            throws Exception {
        return new SqlSessionTemplate(sqlSessionFactory);    
    }
}
