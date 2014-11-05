package mil.nga.giat.geowave.store.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import mil.nga.giat.geowave.index.ByteArrayId;
import mil.nga.giat.geowave.index.NumericIndexStrategy;
import mil.nga.giat.geowave.index.NumericIndexStrategyFactory.DataType;
import mil.nga.giat.geowave.index.dimension.LatitudeDefinition;
import mil.nga.giat.geowave.index.dimension.LongitudeDefinition;
import mil.nga.giat.geowave.index.dimension.NumericDimensionDefinition;
import mil.nga.giat.geowave.index.dimension.TimeDefinition;
import mil.nga.giat.geowave.index.dimension.bin.TemporalBinningStrategy.Unit;
import mil.nga.giat.geowave.index.sfc.SFCFactory.SFCType;
import mil.nga.giat.geowave.index.sfc.tiered.TieredSFCIndexFactory;
import mil.nga.giat.geowave.store.adapter.AbstractDataAdapter;
import mil.nga.giat.geowave.store.adapter.DimensionMatchingIndexFieldHandler;
import mil.nga.giat.geowave.store.adapter.NativeFieldHandler;
import mil.nga.giat.geowave.store.adapter.NativeFieldHandler.RowBuilder;
import mil.nga.giat.geowave.store.adapter.PersistentIndexFieldHandler;
import mil.nga.giat.geowave.store.data.field.BasicReader;
import mil.nga.giat.geowave.store.data.field.BasicReader.GeometryReader;
import mil.nga.giat.geowave.store.data.field.BasicReader.StringReader;
import mil.nga.giat.geowave.store.data.field.BasicWriter;
import mil.nga.giat.geowave.store.data.field.BasicWriter.GeometryWriter;
import mil.nga.giat.geowave.store.data.field.BasicWriter.StringWriter;
import mil.nga.giat.geowave.store.data.field.FieldReader;
import mil.nga.giat.geowave.store.data.field.FieldWriter;
import mil.nga.giat.geowave.store.dimension.GeometryWrapper;
import mil.nga.giat.geowave.store.dimension.Time;
import mil.nga.giat.geowave.store.dimension.TimeField;
import mil.nga.giat.geowave.store.index.CommonIndexModel;
import mil.nga.giat.geowave.store.index.CommonIndexValue;
import mil.nga.giat.geowave.store.index.DimensionalityType;
import mil.nga.giat.geowave.store.index.Index;

import org.junit.Before;
import org.junit.Test;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;

public class PersistenceEncodingTest
{

	private final GeometryFactory factory = new GeometryFactory(
			new PrecisionModel(
					PrecisionModel.FLOATING));

	private static final NumericDimensionDefinition[] SPATIAL_TEMPORAL_DIMENSIONS = new NumericDimensionDefinition[] {
		new LongitudeDefinition(),
		new LatitudeDefinition(),
		new TimeDefinition(
				Unit.YEAR),
	};

	private static final CommonIndexModel model = DimensionalityType.SPATIAL_TEMPORAL.getDefaultIndexModel();

	private static final NumericIndexStrategy strategy = TieredSFCIndexFactory.createSingleTierStrategy(
			SPATIAL_TEMPORAL_DIMENSIONS,
			new int[] {
				16,
				16,
				16
			},
			SFCType.HILBERT);

	final SimpleDateFormat dateFormat = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss.S");

	private static final Index index = new Index(
			strategy,
			model,
			DimensionalityType.SPATIAL_TEMPORAL,
			DataType.OTHER);

	Date start = null, end = null;

	@Before
	public void setUp()
			throws ParseException {

		start = dateFormat.parse("2012-04-03 13:30:23.304");
		end = dateFormat.parse("2012-04-03 14:30:23.304");
	}

	@Test
	public void testPoint() {

		GeoObjDataAdapter adapter = new GeoObjDataAdapter(
				NATIVE_FIELD_HANDLER_LIST,
				COMMON_FIELD_HANDLER_LIST);

		GeoObj entry = new GeoObj(
				factory.createPoint(new Coordinate(
						43.454,
						28.232)),
				start,
				end,
				"g1");
		List<ByteArrayId> ids = adapter.encode(
				entry,
				model).getInsertionIds(
				index);

		assertEquals(
				1,
				ids.size());
	}

	@Test
	public void testLine() {

		GeoObjDataAdapter adapter = new GeoObjDataAdapter(
				NATIVE_FIELD_HANDLER_LIST,
				COMMON_FIELD_HANDLER_LIST);
		GeoObj entry = new GeoObj(
				factory.createLineString(new Coordinate[] {
					new Coordinate(
							43.444,
							28.232),
					new Coordinate(
							43.454,
							28.242)
				}),
				start,
				end,
				"g1");
		List<ByteArrayId> ids = adapter.encode(
				entry,
				model).getInsertionIds(
				index);
		assertEquals(
				7,
				ids.size());

	}

	@Test
	public void testLineWithPrecisionOnTheTileEdge() {

		NumericIndexStrategy strategy = TieredSFCIndexFactory.createSingleTierStrategy(
				SPATIAL_TEMPORAL_DIMENSIONS,
				new int[] {
					14,
					14,
					14
				},
				SFCType.HILBERT);

		Index index = new Index(
				strategy,
				model,
				DimensionalityType.SPATIAL_TEMPORAL,
				DataType.OTHER);

		GeoObjDataAdapter adapter = new GeoObjDataAdapter(
				NATIVE_FIELD_HANDLER_LIST,
				COMMON_FIELD_HANDLER_LIST);
		GeoObj entry = new GeoObj(
				factory.createLineString(new Coordinate[] {
					new Coordinate(
							-99.22,
							33.75000000000001), // notice that
												// this gets
												// tiled as
												// 33.75
					new Coordinate(
							-99.15,
							33.75000000000001)
				// notice that this gets tiled as 33.75
						}),
				new Date(
						352771200000l),
				new Date(
						352771200000l),
				"g1");
		List<ByteArrayId> ids = adapter.encode(
				entry,
				model).getInsertionIds(
				index);
		assertEquals(
				4,
				ids.size());
	}

	@Test
	public void testPoly() {
		GeoObjDataAdapter adapter = new GeoObjDataAdapter(
				NATIVE_FIELD_HANDLER_LIST,
				COMMON_FIELD_HANDLER_LIST);
		GeoObj entry = new GeoObj(
				factory.createLineString(new Coordinate[] {
					new Coordinate(
							43.444,
							28.232),
					new Coordinate(
							43.454,
							28.242),
					new Coordinate(
							43.444,
							28.252),
					new Coordinate(
							43.444,
							28.232),
				}),
				start,
				end,
				"g1");
		List<ByteArrayId> ids = adapter.encode(
				entry,
				model).getInsertionIds(
				index);
		assertEquals(
				18,
				ids.size());
	}

	@Test
	public void testPointRange() {

		GeoObjDataAdapter adapter = new GeoObjDataAdapter(
				NATIVE_FIELD_RANGE_HANDLER_LIST,
				COMMON_FIELD_RANGE_HANDLER_LIST);

		GeoObj entry = new GeoObj(
				factory.createPoint(new Coordinate(
						43.454,
						28.232)),
				start,
				end,
				"g1");
		List<ByteArrayId> ids = adapter.encode(
				entry,
				model).getInsertionIds(
				index);

		assertEquals(
				8,
				ids.size());
	}

	@Test
	public void testLineRnge() {

		GeoObjDataAdapter adapter = new GeoObjDataAdapter(
				NATIVE_FIELD_RANGE_HANDLER_LIST,
				COMMON_FIELD_RANGE_HANDLER_LIST);
		GeoObj entry = new GeoObj(
				factory.createLineString(new Coordinate[] {
					new Coordinate(
							43.444,
							28.232),
					new Coordinate(
							43.454,
							28.242)
				}),
				start,
				end,
				"g1");
		List<ByteArrayId> ids = adapter.encode(
				entry,
				model).getInsertionIds(
				index);
		assertTrue(ids.size() < 100);
	}

	private static final ByteArrayId GEOM = new ByteArrayId(
			"myGeo");
	private static final ByteArrayId ID = new ByteArrayId(
			"myId");
	private static final ByteArrayId START_TIME = new ByteArrayId(
			"startTime");
	private static final ByteArrayId END_TIME = new ByteArrayId(
			"endTime");

	private static final List<NativeFieldHandler<GeoObj, Object>> NATIVE_FIELD_HANDLER_LIST = new ArrayList<NativeFieldHandler<GeoObj, Object>>();
	private static final List<NativeFieldHandler<GeoObj, Object>> NATIVE_FIELD_RANGE_HANDLER_LIST = new ArrayList<NativeFieldHandler<GeoObj, Object>>();
	private static final List<PersistentIndexFieldHandler<GeoObj, ? extends CommonIndexValue, Object>> COMMON_FIELD_HANDLER_LIST = new ArrayList<PersistentIndexFieldHandler<GeoObj, ? extends CommonIndexValue, Object>>();
	private static final List<PersistentIndexFieldHandler<GeoObj, ? extends CommonIndexValue, Object>> COMMON_FIELD_RANGE_HANDLER_LIST = new ArrayList<PersistentIndexFieldHandler<GeoObj, ? extends CommonIndexValue, Object>>();

	private static final NativeFieldHandler<GeoObj, Object> END_TIME_FIELD_HANDLER = new NativeFieldHandler<GeoObj, Object>() {

		@Override
		public ByteArrayId getFieldId() {
			return END_TIME;
		}

		@Override
		public Object getFieldValue(
				final GeoObj row ) {
			return row.endTime;
		}

	};

	private static final NativeFieldHandler<GeoObj, Object> ID_FIELD_HANDLER = new NativeFieldHandler<GeoObj, Object>() {

		@Override
		public ByteArrayId getFieldId() {
			return ID;
		}

		@Override
		public Object getFieldValue(
				final GeoObj row ) {
			return row.id;
		}

	};

	private static final PersistentIndexFieldHandler<GeoObj, ? extends CommonIndexValue, Object> GEOM_FIELD_HANDLER = new PersistentIndexFieldHandler<GeoObj, CommonIndexValue, Object>() {

		@Override
		public ByteArrayId[] getNativeFieldIds() {
			return new ByteArrayId[] {
				GEOM
			};
		}

		@Override
		public CommonIndexValue toIndexValue(
				final GeoObj row ) {
			return new GeometryWrapper(
					row.geometry,
					new byte[0]);
		}

		@Override
		public PersistentValue<Object>[] toNativeValues(
				final CommonIndexValue indexValue ) {
			return new PersistentValue[] {
				new PersistentValue<Object>(
						GEOM,
						((GeometryWrapper) indexValue).getGeometry())
			};
		}

		@Override
		public byte[] toBinary() {
			return new byte[0];
		}

		@Override
		public void fromBinary(
				final byte[] bytes ) {

		}
	};

	static {
		COMMON_FIELD_HANDLER_LIST.add(GEOM_FIELD_HANDLER);
		COMMON_FIELD_HANDLER_LIST.add(new TimeFieldHandler());
		COMMON_FIELD_RANGE_HANDLER_LIST.add(GEOM_FIELD_HANDLER);
		COMMON_FIELD_RANGE_HANDLER_LIST.add(new TimeRangeFieldHandler());
		NATIVE_FIELD_HANDLER_LIST.add(ID_FIELD_HANDLER);
		NATIVE_FIELD_HANDLER_LIST.add(END_TIME_FIELD_HANDLER);
		NATIVE_FIELD_RANGE_HANDLER_LIST.add(ID_FIELD_HANDLER);
	}

	private static class GeoObjDataAdapter extends
			AbstractDataAdapter<GeoObj>
	{

		public GeoObjDataAdapter(
				List<NativeFieldHandler<GeoObj, Object>> nativeFields,
				List<PersistentIndexFieldHandler<GeoObj, ? extends CommonIndexValue, Object>> commonFields ) {
			super(
					commonFields,
					nativeFields);
		}

		@Override
		public ByteArrayId getAdapterId() {
			return new ByteArrayId(
					"geoobj".getBytes());
		}

		@Override
		public boolean isSupported(
				GeoObj entry ) {
			return true;
		}

		@Override
		public ByteArrayId getDataId(
				GeoObj entry ) {
			return new ByteArrayId(
					entry.id.getBytes());
		}

		@Override
		public FieldReader getReader(
				final ByteArrayId fieldId ) {
			if (fieldId.equals(GEOM)) {
				return new GeometryReader();
			}
			else if (fieldId.equals(ID)) {
				return new StringReader();
			}
			else if (fieldId.equals(START_TIME)) {
				return new BasicReader.DateReader();
			}
			else if (fieldId.equals(END_TIME)) {
				return new BasicReader.DateReader();
			}
			return null;
		}

		@Override
		public FieldWriter getWriter(
				final ByteArrayId fieldId ) {
			if (fieldId.equals(GEOM)) {
				return new GeometryWriter();
			}
			else if (fieldId.equals(ID)) {
				return new StringWriter();
			}
			else if (fieldId.equals(START_TIME)) {
				return new BasicWriter.DateWriter();
			}
			else if (fieldId.equals(END_TIME)) {
				return new BasicWriter.DateWriter();
			}
			return null;
		}

		@Override
		protected RowBuilder newBuilder() {
			return new RowBuilder<GeoObj, Object>() {
				private String id;
				private Geometry geom;
				private Date stime;
				private Date etime;

				@Override
				public void setField(
						final PersistentValue<Object> fieldValue ) {
					if (fieldValue.getId().equals(
							GEOM)) {
						geom = (Geometry) fieldValue.getValue();
					}
					else if (fieldValue.getId().equals(
							ID)) {
						id = (String) fieldValue.getValue();
					}
					else if (fieldValue.getId().equals(
							START_TIME)) {
						stime = (Date) fieldValue.getValue();
					}
					else {
						etime = (Date) fieldValue.getValue();
					}
				}

				@Override
				public GeoObj buildRow(
						final ByteArrayId dataId ) {
					return new GeoObj(
							geom,
							stime,
							etime,
							id);
				}
			};
		}

	}

	private static class GeoObj
	{
		private Geometry geometry;
		private String id;
		private Date startTime;
		private Date endTime;

		public GeoObj(
				Geometry geometry,
				Date startTime,
				Date endTime,
				String id ) {
			super();
			this.geometry = geometry;
			this.startTime = startTime;
			this.endTime = endTime;
			this.id = id;
		}

	}

	private static class TimeFieldHandler implements
			PersistentIndexFieldHandler<GeoObj, CommonIndexValue, Object>,
			DimensionMatchingIndexFieldHandler<GeoObj, CommonIndexValue, Object>
	{

		public TimeFieldHandler() {}

		@Override
		public ByteArrayId[] getNativeFieldIds() {
			return new ByteArrayId[] {
				START_TIME
			};
		}

		@Override
		public CommonIndexValue toIndexValue(
				final GeoObj row ) {
			return new Time.Timestamp(
					row.startTime.getTime(),
					new byte[0]);
		}

		@Override
		public PersistentValue<Object>[] toNativeValues(
				final CommonIndexValue indexValue ) {
			return new PersistentValue[] {
				new PersistentValue<Object>(
						START_TIME,
						new Date(
								(long) ((Time.TimeRange) indexValue).toNumericData().getMin())),
				new PersistentValue<Object>(
						END_TIME,
						new Date(
								(long) ((Time.TimeRange) indexValue).toNumericData().getMin()))
			};
		}

		@Override
		public ByteArrayId[] getSupportedIndexFieldIds() {
			return new ByteArrayId[] {
				new TimeField(
						Unit.YEAR).getFieldId()
			};
		}

		@Override
		public byte[] toBinary() {
			return new byte[0];
		}

		@Override
		public void fromBinary(
				final byte[] bytes ) {

		}
	};

	private static class TimeRangeFieldHandler implements
			PersistentIndexFieldHandler<GeoObj, CommonIndexValue, Object>,
			DimensionMatchingIndexFieldHandler<GeoObj, CommonIndexValue, Object>
	{

		public TimeRangeFieldHandler() {}

		@Override
		public ByteArrayId[] getNativeFieldIds() {
			return new ByteArrayId[] {
				START_TIME,
				END_TIME
			};
		}

		@Override
		public CommonIndexValue toIndexValue(
				final GeoObj row ) {
			return new Time.TimeRange(
					row.startTime.getTime(),
					row.endTime.getTime(),
					new byte[0]);
		}

		@Override
		public PersistentValue<Object>[] toNativeValues(
				final CommonIndexValue indexValue ) {
			return new PersistentValue[] {
				new PersistentValue<Object>(
						START_TIME,
						new Date(
								(long) ((Time.TimeRange) indexValue).toNumericData().getMin())),
				new PersistentValue<Object>(
						END_TIME,
						new Date(
								(long) ((Time.TimeRange) indexValue).toNumericData().getMin()))
			};
		}

		@Override
		public ByteArrayId[] getSupportedIndexFieldIds() {
			return new ByteArrayId[] {
				new TimeField(
						Unit.YEAR).getFieldId()
			};
		}

		@Override
		public byte[] toBinary() {
			return new byte[0];
		}

		@Override
		public void fromBinary(
				final byte[] bytes ) {

		}
	};

}
