/*
 * Copyright JAMF Software, LLC
 */

package com.jamf.regatta.data.convert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.jamf.regatta.core.api.ByteSequence;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;

import static com.jamf.regatta.data.configuration.RegattaRepositoryConfigurationExtension.REGATTA_OBJECT_MAPPER_BEAN_NAME;
import static com.jamf.regatta.data.configuration.RegattaRepositoryConfigurationExtension.REGATTA_XML_MAPPER_BEAN_NAME;

public class RegattaConverter implements InitializingBean, ApplicationContextAware {

    private final DefaultConversionService converter = new DefaultConversionService();
    private ApplicationContext applicationContext;

    public RegattaConverter() {
        ByteSequenceConverters.converters().forEach(converter::addConverter);
    }

    public ByteSequence write(Object source) {
        return converter.convert(source, ByteSequence.class);
    }

    public <T> T read(ByteSequence source, Class<T> clazz) {
        return converter.convert(source, clazz);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        converter.addConverter(new JsonConverterFactory((ObjectMapper) this.applicationContext.getBean(REGATTA_OBJECT_MAPPER_BEAN_NAME)));
        converter.addConverter(new XmlConverterFactory((XmlMapper) this.applicationContext.getBean(REGATTA_XML_MAPPER_BEAN_NAME)));
        this.applicationContext.getBeansOfType(Converter.class).values().forEach(converter::addConverter);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
