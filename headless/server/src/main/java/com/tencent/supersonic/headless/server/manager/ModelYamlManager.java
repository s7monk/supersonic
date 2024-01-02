package com.tencent.supersonic.headless.server.manager;

import com.tencent.supersonic.headless.common.server.enums.ModelSourceType;
import com.tencent.supersonic.headless.common.server.pojo.Dim;
import com.tencent.supersonic.headless.common.server.pojo.Identify;
import com.tencent.supersonic.headless.common.server.pojo.Measure;
import com.tencent.supersonic.headless.common.server.pojo.ModelDetail;
import com.tencent.supersonic.headless.common.server.response.DatabaseResp;
import com.tencent.supersonic.headless.common.server.response.ModelResp;
import com.tencent.supersonic.headless.server.pojo.yaml.DataModelYamlTpl;
import com.tencent.supersonic.headless.server.pojo.yaml.DimensionTimeTypeParamsTpl;
import com.tencent.supersonic.headless.server.pojo.yaml.DimensionYamlTpl;
import com.tencent.supersonic.headless.server.pojo.yaml.IdentifyYamlTpl;
import com.tencent.supersonic.headless.server.pojo.yaml.MeasureYamlTpl;
import com.tencent.supersonic.headless.server.engineadapter.EngineAdaptor;
import com.tencent.supersonic.headless.server.engineadapter.EngineAdaptorFactory;
import com.tencent.supersonic.headless.server.pojo.DatasourceQueryEnum;
import com.tencent.supersonic.headless.server.utils.SysTimeDimensionBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Objects;
import java.util.stream.Collectors;


@Service
@Slf4j
public class ModelYamlManager {

    public static DataModelYamlTpl convert2YamlObj(ModelResp modelResp, DatabaseResp databaseResp) {
        ModelDetail modelDetail = modelResp.getModelDetail();
        EngineAdaptor engineAdaptor = EngineAdaptorFactory.getEngineAdaptor(databaseResp.getType());
        SysTimeDimensionBuilder.addSysTimeDimension(modelDetail.getDimensions(), engineAdaptor);
        addInterCntMetric(modelResp.getBizName(), modelDetail);
        DataModelYamlTpl dataModelYamlTpl = new DataModelYamlTpl();
        BeanUtils.copyProperties(modelDetail, dataModelYamlTpl);
        dataModelYamlTpl.setIdentifiers(modelDetail.getIdentifiers().stream().map(ModelYamlManager::convert)
                .collect(Collectors.toList()));
        dataModelYamlTpl.setDimensions(modelDetail.getDimensions().stream().map(ModelYamlManager::convert)
                .collect(Collectors.toList()));
        dataModelYamlTpl.setMeasures(modelDetail.getMeasures().stream().map(ModelYamlManager::convert)
                .collect(Collectors.toList()));
        dataModelYamlTpl.setName(modelResp.getBizName());
        dataModelYamlTpl.setSourceId(modelResp.getDatabaseId());
        dataModelYamlTpl.setModelSourceTypeEnum(ModelSourceType.of(modelResp.getSourceType()));
        if (modelDetail.getQueryType().equalsIgnoreCase(DatasourceQueryEnum.SQL_QUERY.getName())) {
            dataModelYamlTpl.setSqlQuery(modelDetail.getSqlQuery());
        } else {
            dataModelYamlTpl.setTableQuery(modelDetail.getTableQuery());
        }
        return dataModelYamlTpl;
    }

    public static DimensionYamlTpl convert(Dim dim) {
        DimensionYamlTpl dimensionYamlTpl = new DimensionYamlTpl();
        BeanUtils.copyProperties(dim, dimensionYamlTpl);
        dimensionYamlTpl.setName(dim.getBizName());
        if (Objects.isNull(dimensionYamlTpl.getExpr())) {
            dimensionYamlTpl.setExpr(dim.getBizName());
        }
        if (dim.getTypeParams() != null) {
            DimensionTimeTypeParamsTpl dimensionTimeTypeParamsTpl = new DimensionTimeTypeParamsTpl();
            dimensionTimeTypeParamsTpl.setIsPrimary(dim.getTypeParams().getIsPrimary());
            dimensionTimeTypeParamsTpl.setTimeGranularity(dim.getTypeParams().getTimeGranularity());
            dimensionYamlTpl.setTypeParams(dimensionTimeTypeParamsTpl);
        }
        return dimensionYamlTpl;
    }

    public static MeasureYamlTpl convert(Measure measure) {
        MeasureYamlTpl measureYamlTpl = new MeasureYamlTpl();
        BeanUtils.copyProperties(measure, measureYamlTpl);
        measureYamlTpl.setName(measure.getBizName());
        return measureYamlTpl;
    }

    public static IdentifyYamlTpl convert(Identify identify) {
        IdentifyYamlTpl identifyYamlTpl = new IdentifyYamlTpl();
        identifyYamlTpl.setName(identify.getBizName());
        identifyYamlTpl.setType(identify.getType());
        return identifyYamlTpl;
    }

    private static void addInterCntMetric(String datasourceEnName, ModelDetail datasourceDetail) {
        Measure measure = new Measure();
        measure.setExpr("1");
        if (!CollectionUtils.isEmpty(datasourceDetail.getIdentifiers())) {
            measure.setExpr(datasourceDetail.getIdentifiers().get(0).getBizName());
        }
        measure.setAgg("count");
        measure.setBizName(String.format("%s_%s", datasourceEnName, "internal_cnt"));
        measure.setCreateMetric("true");
        datasourceDetail.getMeasures().add(measure);
    }

}