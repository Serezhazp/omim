#pragma once
#include "indexer/mercator.hpp"
#include "indexer/point_to_int64.hpp"

#include "base/assert.hpp"


template <int MinX, int MinY, int MaxX, int MaxY>
struct Bounds
{
  enum
  {
    minX = MinX,
    maxX = MaxX,
    minY = MinY,
    maxY = MaxY
  };
};

//typedef Bounds<-180, -90, 180, 90> OrthoBounds;

template <typename BoundsT, typename CellIdT>
class CellIdConverter
{
public:
  static double XToCellIdX(double x)
  {
    return (x - BoundsT::minX) / StepX();
  }
  static double YToCellIdY(double y)
  {
    return (y - BoundsT::minY) / StepY();
  }

  static double CellIdXToX(double  x)
  {
    return (x*StepX() + BoundsT::minX);
  }
  static double CellIdYToY(double y)
  {
    return (y*StepY() + BoundsT::minY);
  }

  static CellIdT ToCellId(double x, double y)
  {
    uint32_t const ix = static_cast<uint32_t>(XToCellIdX(x));
    uint32_t const iy = static_cast<uint32_t>(YToCellIdY(y));
    CellIdT id = CellIdT::FromXY(ix, iy, CellIdT::DEPTH_LEVELS - 1);
#if 0 // DEBUG
    pair<uint32_t, uint32_t> ixy = id.XY();
    ASSERT(Abs(ixy.first  - ix) <= 1, (x, y, id, ixy));
    ASSERT(Abs(ixy.second - iy) <= 1, (x, y, id, ixy));
    CoordT minX, minY, maxX, maxY;
    GetCellBounds(id, minX, minY, maxX, maxY);
    ASSERT(minX <= x && x <= maxX, (x, y, id, minX, minY, maxX, maxY));
    ASSERT(minY <= y && y <= maxY, (x, y, id, minX, minY, maxX, maxY));
#endif
    return id;
  }

  static CellIdT Cover2PointsWithCell(double x1, double y1, double x2, double y2)
  {
    CellIdT id1 = ToCellId(x1, y1);
    CellIdT id2 = ToCellId(x2, y2);
    while (id1 != id2)
    {
      id1 = id1.Parent();
      id2 = id2.Parent();
    }
#if 0 // DEBUG
    CoordT minX, minY, maxX, maxY;
    GetCellBounds(id1, minX, minY, maxX, maxY);
    ASSERT(minX <= x1 && x1 <= maxX, (x1, y1, x2, y2, id1, minX, minY, maxX, maxY));
    ASSERT(minX <= x2 && x2 <= maxX, (x1, y1, x2, y2, id1, minX, minY, maxX, maxY));
    ASSERT(minY <= y1 && y1 <= maxY, (x1, y1, x2, y2, id1, minX, minY, maxX, maxY));
    ASSERT(minY <= y2 && y2 <= maxY, (x1, y1, x2, y2, id1, minX, minY, maxX, maxY));
#endif
    return id1;
  }

  static m2::PointD FromCellId(CellIdT id)
  {
    pair<uint32_t, uint32_t> const xy = id.XY();
    return m2::PointD(CellIdXToX(xy.first), CellIdYToY(xy.second));
  }

  static void GetCellBounds(CellIdT id,
                            double & minX, double & minY, double & maxX, double & maxY)
  {
    pair<uint32_t, uint32_t> const xy = id.XY();
    uint32_t const r = id.Radius();
    minX = (xy.first - r) * StepX() + BoundsT::minX;
    maxX = (xy.first + r) * StepX() + BoundsT::minX;
    minY = (xy.second - r) * StepY() + BoundsT::minY;
    maxY = (xy.second + r) * StepY() + BoundsT::minY;
  }

private:
  inline static double StepX()
  {
    return double(BoundsT::maxX - BoundsT::minX) / CellIdT::MAX_COORD;
  }
  inline static double StepY()
  {
    return double(BoundsT::maxY - BoundsT::minY) / CellIdT::MAX_COORD;
  }
};
