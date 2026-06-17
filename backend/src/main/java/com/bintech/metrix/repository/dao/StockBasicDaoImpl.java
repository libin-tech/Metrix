package com.bintech.metrix.repository.dao;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bintech.metrix.repository.entity.StockBasic;
import com.bintech.metrix.repository.mapper.StockBasicMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
class StockBasicDaoImpl implements StockBasicDao {

    private final StockBasicMapper baseMapper;

    @Override
    public int insert(StockBasic entity) {
        return baseMapper.insert(entity);
    }

    @Override
    public int updateById(StockBasic entity) {
        return baseMapper.updateById(entity);
    }

    @Override
    public int deleteById(Long id) {
        return baseMapper.deleteById(id);
    }

    @Override
    public StockBasic selectById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public StockBasic selectByTsCode(String tsCode) {
        if (StrUtil.isBlank(tsCode)) {
            log.warn("selectByTsCode: tsCode is blank");
            return null;
        }
        return baseMapper.selectOne(new LambdaQueryWrapper<StockBasic>().eq(StockBasic::getTsCode, tsCode));
    }

    @Override
    public StockBasic selectBySymbol(String symbol) {
        if (StrUtil.isBlank(symbol)) {
            log.warn("selectBySymbol: symbol is blank");
            return null;
        }
        return baseMapper.selectOne(new LambdaQueryWrapper<StockBasic>().eq(StockBasic::getSymbol, symbol));
    }

    @Override
    public StockBasic selectByName(String name) {
        if (StrUtil.isBlank(name)) {
            log.warn("selectByName: name is blank");
            return null;
        }
        return baseMapper.selectOne(new LambdaQueryWrapper<StockBasic>()
                .eq(StockBasic::getName, name).last("LIMIT 1"));
    }

    @Override
    public StockBasic selectByNameLike(String name) {
        if (StrUtil.isBlank(name)) {
            log.warn("selectByNameLike: name is blank");
            return null;
        }
        return baseMapper.selectOne(new LambdaQueryWrapper<StockBasic>()
                .like(StockBasic::getName, name).last("LIMIT 1"));
    }

    @Override
    public List<StockBasic> selectByTsCodeIn(List<String> tsCodes) {
        if (CollUtil.isEmpty(tsCodes)) {
            log.warn("selectByTsCodeIn: tsCodes is empty");
            return List.of();
        }
        return baseMapper.selectList(new LambdaQueryWrapper<StockBasic>().in(StockBasic::getTsCode, tsCodes));
    }

    @Override
    public List<StockBasic> selectLikeNameOrTsCode(String keyword) {
        if (StrUtil.isBlank(keyword)) {
            log.warn("selectLikeNameOrTsCode: keyword is blank");
            return List.of();
        }
        return baseMapper.selectList(new LambdaQueryWrapper<StockBasic>()
                .like(StockBasic::getName, keyword)
                .or().like(StockBasic::getTsCode, keyword)
                .last("LIMIT 20"));
    }

    @Override
    public IPage<StockBasic> selectStockPage(Page<StockBasic> page, String keyword) {
        LambdaQueryWrapper<StockBasic> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.like(StockBasic::getName, keyword)
                    .or().like(StockBasic::getTsCode, keyword);
        }
        wrapper.orderByAsc(StockBasic::getId);
        return baseMapper.selectPage(page, wrapper);
    }
}
