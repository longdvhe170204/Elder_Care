package com.cambae.backend.m15.account.internal.permission.service;

import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.FieldGroup;
import com.cambae.backend.m15.account.api.FieldVisibilityApi;
import com.cambae.backend.m15.account.api.VisibilityTarget;
import com.cambae.backend.m15.account.api.dto.InfoGroup;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.account.api.dto.Visibility;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.ValueSerializerModifier;

/**
 * Giới hạn trường khi tuần tự hóa (002 T023; research A-08; FR-022): thuộc tính mang {@link FieldGroup} chỉ được ghi
 * khi người dùng hiện tại xem đầy đủ nhóm đó trên đối tượng của DTO ({@link VisibilityTarget}); thuộc tính
 * {@code hiddenFields} của DTO được ghi lại bằng danh sách tên trường đã bỏ. Mức "chỉ có/không" cũng bỏ trường: DTO tự
 * mang cờ có/không (ví dụ {@code coNguyenVong} của 001). Không xác định được người dùng thì bỏ mọi trường có nhóm.
 */
public class FieldGroupSerializerModifier extends ValueSerializerModifier {

    public static final String HIDDEN_FIELDS = "hiddenFields";

    private final ObjectProvider<CurrentUser> currentUser;
    private final ObjectProvider<FieldVisibilityApi> visibility;

    public FieldGroupSerializerModifier(
            ObjectProvider<CurrentUser> currentUser, ObjectProvider<FieldVisibilityApi> visibility) {
        this.currentUser = currentUser;
        this.visibility = visibility;
    }

    @Override
    public List<BeanPropertyWriter> changeProperties(
            SerializationConfig config, BeanDescription.Supplier beanDesc, List<BeanPropertyWriter> properties) {
        Map<String, InfoGroup> grouped = new LinkedHashMap<>();
        for (BeanPropertyWriter property : properties) {
            FieldGroup group = property.getAnnotation(FieldGroup.class);
            if (group != null) {
                grouped.put(property.getName(), group.value());
            }
        }
        if (grouped.isEmpty()) {
            return properties;
        }
        List<BeanPropertyWriter> result = new ArrayList<>(properties.size());
        for (BeanPropertyWriter property : properties) {
            if (grouped.containsKey(property.getName())) {
                result.add(new GroupedWriter(property, grouped.get(property.getName())));
            } else if (property.getName().equals(HIDDEN_FIELDS)) {
                result.add(new HiddenFieldsWriter(property, grouped));
            } else {
                result.add(property);
            }
        }
        return result;
    }

    private boolean visible(Object bean, InfoGroup group) {
        Target target = bean instanceof VisibilityTarget t ? t.visibilityTarget() : Target.UNSPECIFIED;
        try {
            return visibility.getObject().canView(currentUser.getObject(), group, target) == Visibility.FULL;
        } catch (IllegalStateException noCurrentUser) {
            return false;
        }
    }

    /** Thuộc tính có nhóm: chỉ ghi khi được xem đầy đủ. */
    private final class GroupedWriter extends BeanPropertyWriter {

        private final InfoGroup group;

        GroupedWriter(BeanPropertyWriter base, InfoGroup group) {
            super(base);
            this.group = group;
        }

        @Override
        public void serializeAsProperty(Object bean, JsonGenerator gen, SerializationContext ctxt) throws Exception {
            if (visible(bean, group)) {
                super.serializeAsProperty(bean, gen, ctxt);
            }
        }
    }

    /** {@code hiddenFields}: tên các thuộc tính có nhóm đã bỏ, theo thứ tự khai báo. */
    private final class HiddenFieldsWriter extends BeanPropertyWriter {

        private final Map<String, InfoGroup> grouped;

        HiddenFieldsWriter(BeanPropertyWriter base, Map<String, InfoGroup> grouped) {
            super(base);
            this.grouped = grouped;
        }

        @Override
        public void serializeAsProperty(Object bean, JsonGenerator gen, SerializationContext ctxt) {
            gen.writeName(getName());
            gen.writeStartArray();
            grouped.forEach((name, group) -> {
                if (!visible(bean, group)) {
                    gen.writeString(name);
                }
            });
            gen.writeEndArray();
        }
    }
}
