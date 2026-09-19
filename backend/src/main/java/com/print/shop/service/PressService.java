package com.print.shop.service;

import com.print.shop.dto.BizException;
import com.print.shop.entity.Press;
import com.print.shop.repository.PlateRepository;
import com.print.shop.repository.PressRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PressService {

    private final PressRepository presses;
    private final PlateRepository plates;

    public PressService(PressRepository presses, PlateRepository plates) {
        this.presses = presses;
        this.plates = plates;
    }

    public List<Press> list(String state, String keyword) {
        return presses.findAllByOrderByIdAsc().stream()
                .filter(p -> state == null || state.isBlank() || state.equals(p.pressState))
                .filter(p -> keyword == null || keyword.isBlank()
                        || p.pressCode.contains(keyword) || p.pressName.contains(keyword))
                .toList();
    }

    @Transactional
    public Press save(Press form) {
        Press origin = null;
        if (form.id != null) {
            origin = presses.findById(form.id).orElseThrow(() -> new BizException("印刷机不存在"));
            if (form.pressCode == null || form.pressCode.isBlank()) {
                form.pressCode = origin.pressCode;
            }
            if (form.pressName == null || form.pressName.isBlank()) {
                form.pressName = origin.pressName;
            }
        }
        if (form.pressCode == null || form.pressCode.isBlank()) {
            throw new BizException("印刷机编号不能空着");
        }
        if (form.pressName == null || form.pressName.isBlank()) {
            throw new BizException("印刷机名称不能空着");
        }
        form.pressCode = form.pressCode.trim();
        presses.findByPressCode(form.pressCode).ifPresent(other -> {
            if (!other.id.equals(form.id)) {
                throw new BizException("编号 " + form.pressCode + " 已经用在别的机器上了");
            }
        });
        if (origin == null) {
            form.pressState = form.pressState == null || form.pressState.isBlank() ? "停机" : form.pressState;
            return presses.save(form);
        }
        if ("封存".equals(form.pressState) && !"封存".equals(origin.pressState)
                && plates.countByPressId(form.id) > 0) {
            throw new BizException("这台机器上还装着印版，先卸版再封存");
        }
        if (form.modelText != null && !form.modelText.isBlank()) {
            origin.modelText = form.modelText;
        }
        if (form.operator != null && !form.operator.isBlank()) {
            origin.operator = form.operator;
        }
        if (form.pressState != null && !form.pressState.isBlank()) {
            origin.pressState = form.pressState;
        }
        origin.pressCode = form.pressCode;
        origin.pressName = form.pressName;
        return presses.save(origin);
    }
}
