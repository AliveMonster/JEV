package com.example.fraudpoc.dataset;

import com.example.fraudpoc.config.AppProperties;
import com.example.fraudpoc.model.DatasetRecord;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

@Service
public class DatasetLoader {
    private final ObjectMapper mapper;
    private final AppProperties props;
    public DatasetLoader(ObjectMapper mapper, AppProperties props){this.mapper=mapper;this.props=props;}
    public List<DatasetRecord> load(){
        try(InputStream in=getClass().getResourceAsStream(props.getBenchmark().getDataset())){
            if(in==null) throw new IllegalStateException("Dataset not found: "+props.getBenchmark().getDataset());
            return mapper.readValue(in, new TypeReference<>(){});
        } catch(Exception e){throw new IllegalStateException("Unable to load dataset",e);}
    }
}
