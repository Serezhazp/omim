#pragma once

#include "drape_frontend/stylist.hpp"
#include "drape_frontend/tile_key.hpp"

#include "indexer/point_to_int64.hpp"

#include "geometry/point2d.hpp"
#include "geometry/spline.hpp"

class CircleRuleProto;
class SymbolRuleProto;
class CaptionDefProto;

namespace df
{

struct TextViewParams;
class EngineContext;

class BaseApplyFeature
{
public:
  BaseApplyFeature(EngineContext & context,
                   TileKey tileKey,
                   FeatureID const & id,
                   CaptionDescription const & captions);

protected:
  void ExtractCaptionParams(CaptionDefProto const * primaryProto,
                            CaptionDefProto const * secondaryProto,
                            double depth,
                            TextViewParams & params) const;

protected:
  EngineContext & m_context;
  TileKey m_tileKey;
  FeatureID m_id;
  CaptionDescription const & m_captions;
};

class ApplyPointFeature : public BaseApplyFeature
{
  typedef BaseApplyFeature TBase;
public:
  ApplyPointFeature(EngineContext & context,
                    TileKey tileKey,
                    FeatureID const & id,
                    CaptionDescription const & captions);

  void operator()(m2::PointD const & point);
  void ProcessRule(Stylist::rule_wrapper_t const & rule);
  void Finish();

private:
  bool m_hasPoint;
  double m_symbolDepth;
  double m_circleDepth;
  SymbolRuleProto const * m_symbolRule;
  CircleRuleProto const * m_circleRule;
  m2::PointF m_centerPoint;
};

class ApplyAreaFeature : public ApplyPointFeature
{
  typedef ApplyPointFeature TBase;
public:
  ApplyAreaFeature(EngineContext & context,
                   TileKey tileKey,
                   FeatureID const & id,
                   CaptionDescription const & captions);

  using TBase::operator ();

  void operator()(m2::PointD const & p1, m2::PointD const & p2, m2::PointD const & p3);
  void ProcessRule(Stylist::rule_wrapper_t const & rule);

private:
  vector<m2::PointF> m_triangles;
};

class ApplyLineFeature : public BaseApplyFeature
{
  typedef BaseApplyFeature TBase;
public:
  ApplyLineFeature(EngineContext & context,
                   TileKey tileKey,
                   FeatureID const & id,
                   CaptionDescription const & captions,
                   double currentScaleGtoP);

  void operator() (m2::PointD const & point);
  bool HasGeometry() const;
  void ProcessRule(Stylist::rule_wrapper_t const & rule);
  void Finish();

private:
  m2::SharedSpline m_spline;
  double m_currentScaleGtoP;
};

} // namespace df
