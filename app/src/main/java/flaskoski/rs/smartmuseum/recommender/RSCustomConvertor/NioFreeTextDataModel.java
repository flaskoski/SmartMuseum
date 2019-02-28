package flaskoski.rs.smartmuseum.recommender.RSCustomConvertor;

/**
 * Copyright (C) 2016 LibRec
 * <p>
 * This file is part of LibRec.
 * LibRec is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * LibRec is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with LibRec. If not, see <http://www.gnu.org/licenses/>.
 */

import android.content.Context;

import com.google.common.collect.BiMap;

import net.librec.common.LibrecException;
import net.librec.conf.Configuration;
import net.librec.conf.Configured;
import net.librec.data.DataModel;
import net.librec.math.structure.DataSet;

import java.io.IOException;

/**
 * A <tt>TextDataModel</tt> represents a data access class to the CSV format
 * input.
 *
 * @author WangYuFeng
 */
public class NioFreeTextDataModel extends NioFreeAbstractDataModel implements DataModel {

    /**
     * Empty constructor.
     * @param conf
     * @param applicationContext
     */
    public NioFreeTextDataModel(Configuration conf, Context applicationContext) {
        this.conf = conf;
        this.applicationContex = applicationContext;
    }

    /**
     * Initializes a newly created {@code TextDataModel} object with
     * configuration.
     *
     * @param conf
     *            the configuration for the model.
     */
    public NioFreeTextDataModel(Configuration conf) {
        this.conf = conf;
    }

    /**
     * Build Convert.
     *
     * @throws LibrecException
     *             if error occurs during building
     */
    @Override
    public void buildConvert() throws LibrecException {
        String inputDataPath = conf.get(Configured.CONF_DFS_DATA_DIR) /*+ File.separator */+ conf.get(Configured.CONF_DATA_INPUT_PATH);
        String dataColumnFormat = conf.get(Configured.CONF_DATA_COLUMN_FORMAT, "UIR");
        dataConvertor = new NioFreeTextDataConvertor(dataColumnFormat, inputDataPath, conf.getDouble("data.convert.binarize.threshold", -1.0), applicationContex);
        try {
            dataConvertor.processData();
        } catch (IOException e) {
            throw new LibrecException(e);
        }
    }

    /**
     * Load data model.
     *
     * @throws LibrecException
     *             if error occurs during loading
     */
    @Override
    public void loadDataModel() throws LibrecException {
        conf.setBoolean("data.convert.read.ready", false);
        buildDataModel();
    }

    /**
     * Save data model.
     *
     * @throws LibrecException
     *             if error occurs during saving
     */
    @Override
    public void saveDataModel() throws LibrecException {

    }

    /**
     * Get user mapping data.
     *
     * @return the user {raw id, inner id} map of data model.
     */
    @Override
    public BiMap<String, Integer> getUserMappingData() {
        return ((NioFreeTextDataConvertor) dataConvertor).getUserIds();
    }

    /**
     * Get item mapping data.
     *
     * @return the item {raw id, inner id} map of data model.
     */
    @Override
    public BiMap<String, Integer> getItemMappingData() {
        return ((NioFreeTextDataConvertor) dataConvertor).getItemIds();
    }

    /**
     * Get datetime data set.
     *
     * @return the datetime data set of data model.
     */
    @Override
    public DataSet getDatetimeDataSet() {
        return ((NioFreeTextDataConvertor) dataConvertor).getDatetimeMatrix();
    }
}
