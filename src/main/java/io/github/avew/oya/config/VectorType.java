package io.github.avew.oya.config;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.*;
import java.util.Objects;

/**
 * Custom Hibernate type for PostgreSQL vector (pgvector) columns
 */
public class VectorType implements UserType<String> {

    @Override
    public int getSqlType() {
        return Types.OTHER;
    }

    @Override
    public Class<String> returnedClass() {
        return String.class;
    }

    @Override
    public boolean equals(String x, String y) throws HibernateException {
        return Objects.equals(x, y);
    }

    @Override
    public int hashCode(String x) throws HibernateException {
        return Objects.hashCode(x);
    }

    @Override
    public String nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner)
            throws HibernateException, SQLException {
        String value = rs.getString(position);
        return rs.wasNull() ? null : value;
    }

    @Override
    public void nullSafeSet(PreparedStatement st, String value, int index, SharedSessionContractImplementor session)
            throws HibernateException, SQLException {
        if (value == null) {
            st.setNull(index, Types.OTHER);
        } else {
            // Set the vector value using PostgreSQL's vector type
            st.setObject(index, value, Types.OTHER);
        }
    }

    @Override
    public String deepCopy(String value) throws HibernateException {
        return value;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(String value) throws HibernateException {
        return value;
    }

    @Override
    public String assemble(Serializable cached, Object owner) throws HibernateException {
        return (String) cached;
    }

    @Override
    public String replace(String original, String target, Object owner) throws HibernateException {
        return original;
    }
}
