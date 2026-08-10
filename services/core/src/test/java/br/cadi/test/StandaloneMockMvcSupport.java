package br.cadi.test;

import br.cadi.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * MockMvc standalone com Jackson, validação Bean Validation e conversão de datas —
 * necessário porque {@link MockMvcBuilders#standaloneSetup(Object)} não registra
 * conversores HTTP por padrão.
 */
public final class StandaloneMockMvcSupport {

    /**
     * Replica o ObjectMapper auto-configurado pelo Spring Boot: inclui módulos para
     * {@code java.time} e {@code ParameterNamesModule} (necessário p/ desserializar
     * records e construtores @JsonCreator implicitos sem perder nomes de parâmetros).
     */
    private static final ObjectMapper OBJECT_MAPPER = Jackson2ObjectMapperBuilder.json()
            .modules(new JavaTimeModule(), new ParameterNamesModule())
            .build();

    private StandaloneMockMvcSupport() {
    }

    public static ObjectMapper objectMapper() {
        return OBJECT_MAPPER;
    }

    public static MockMvc standaloneSetup(Object... controllers) {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
        jsonConverter.setObjectMapper(OBJECT_MAPPER);

        return MockMvcBuilders.standaloneSetup(controllers)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(jsonConverter)
                .setValidator(validator)
                .setConversionService(new DefaultFormattingConversionService())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }
}
